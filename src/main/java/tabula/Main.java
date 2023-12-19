package tabula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.detectors.SpreadsheetDetectionAlgorithm;



public class Main {
	
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_CYAN = "\u001B[36m";

	public static void main(String[] args) throws IOException {
		


		long start = System.currentTimeMillis();
		
		String[] extentions = { "pdf" };
		String basePath = "C:\\Users\\George\\Desktop\\pdf";
		Iterator<File> it = FileUtils.iterateFiles(new File(basePath), extentions, false);

		File extractedFolder = new File(basePath + "\\extracted");
		if (extractedFolder.exists())
			throw new IOException("\"extracted\" folder already exists");
		else
			extractedFolder.mkdirs();

		NurminenDetectionAlgorithm nurminenAlgorithm = new NurminenDetectionAlgorithm();
		SpreadsheetDetectionAlgorithm spreadAlgorithm = new SpreadsheetDetectionAlgorithm();

		while (it.hasNext()) {

			File pdfFile = it.next();
			System.out.println(pdfFile.getName());
			InputStream in = new FileInputStream(pdfFile);

			try (PDDocument document = PDDocument.load(in)) {

				List<Integer> pagesWithTables = findTablesAndScannedPages(document, spreadAlgorithm, nurminenAlgorithm);

				Map<Integer, Boolean> map = convertArrayListToSortedMap(pagesWithTables, document.getNumberOfPages());

				map.forEach((k, v) -> System.out.println("pageNumber: " + (k) + " hasTable: " + v));

				String extractedPdfPath = basePath + "\\extracted\\" + FilenameUtils.getBaseName(pdfFile.getName())
						+ "_extracted.pdf";

//				createExtractedPDF(document, map, extractedPdfPath);
				createPDFWithOverlay(document, map, extractedPdfPath);

			}
		}
		long finish = System.currentTimeMillis();
		long timeElapsed = finish - start;
		System.out.println("time elapsed for file: " + timeElapsed / (double) 1000 + " s");
	}

	
	/**
	 * Takes a {@link org.apache.pdfbox.pdmodel.PDDocument PDDocument} 
	 * and some {@link technology.tabula.detectors.DetectionAlgorithm DetectionAlgorithms} from tabula library 
	 * and returns a list of page numbers in 1-based form of the pages 
	 * that have tables or are completely scanned (not partially scanned with some text)
	 * 
	 * @param document
	 * @param spreadAlgorithm
	 * @param nurminenAlgorithm
	 * @return List of Integers: pagesWithTables and scanned pages
	 */
	public static List<Integer> findTablesAndScannedPages(PDDocument document, SpreadsheetDetectionAlgorithm spreadAlgorithm,
			NurminenDetectionAlgorithm nurminenAlgorithm) {
		List<Integer> pagesWithTables = new ArrayList<>();

		// if you close this, it closes the document, tabula is written in that way. 
		@SuppressWarnings("resource")
		ObjectExtractor oe = new ObjectExtractor(document);
		PageIterator pageIterator = oe.extract();

		int i = 1;
		while (pageIterator.hasNext()) {

			Page page = pageIterator.next();

			System.out.println(ANSI_YELLOW + "Page: " + (i) + ANSI_RESET);

			// discard empty pages
			if (!document.getPage(i - 1).hasContents()) {
				i++;
				continue;
			}

			// add scanned pages, !page.hasText() produces the same result but is deprecated
			if (page.getText().isEmpty()) {
				pagesWithTables.add(i);
				System.out.println(ANSI_GREEN + "Page: " + i + " is scanned!!!!!!" + ANSI_RESET);
			}

			// spreadAlgorithm is evaluated first because it gives the best results and is
			// faster. the other has some edge cases
			else if (!spreadAlgorithm.detect(page).isEmpty() || !nurminenAlgorithm.detect(page).isEmpty()) {
				pagesWithTables.add(i);
			}
			i++;
		}
		return pagesWithTables;
	}
	
	
	/**
	 * Takes a {@link org.apache.pdfbox.pdmodel.PDDocument PDDocument}, 
	 * a map with 1-based page indexes keys and boolean values, and a path
	 * and creates a new pdf from the original as the map describes in the provided path  
	 * @param document
	 * @param map
	 * @param extractedPdfPath
	 * @throws IOException
	 */
	public static void createExtractedPDF(PDDocument document, Map<Integer, Boolean> map,
			String extractedPdfPath) throws IOException {
		File extractedFile = new File(extractedPdfPath);
		
		System.out.println(extractedFile.getPath());
		
		try (PDDocument extracted = new PDDocument()) {

			map.forEach((pageNumber, hasTables) -> {
				if (hasTables.booleanValue()) {
					extracted.addPage(document.getPage(pageNumber - 1));
				} else {
					PDRectangle dimensions = document.getPage(pageNumber - 1).getMediaBox();
					extracted.addPage(new PDPage(dimensions));
				}
			});

			extracted.save(extractedFile);
		}
	}
	
	
	/**
	 * Takes a {@link org.apache.pdfbox.pdmodel.PDDocument PDDocument}, 
	 * a map with 1-based page indexes keys and boolean values, and a path
	 * and creates a new pdf with the excluded pages crossed with a watermark in the provided path  
	 * @param document
	 * @param map
	 * @param extractedPdfPath
	 * @throws IOException
	 */
	public static void createPDFWithOverlay(PDDocument document, Map<Integer, Boolean> map,
			String extractedPdfPath) throws IOException{
		
		
		Map<Integer, String> overlayGuide = mapToOverlayGuide(map);
	   
		Overlay overlay = new Overlay();
		
		overlay.setInputPDF(document);
		overlay.overlay(overlayGuide).save(extractedPdfPath);
		overlay.close();
	}
	
	
	public static Map<Integer, String> mapToOverlayGuide(Map<Integer, Boolean> map) {
		
		Map<Integer, String> overlayGuide = new HashMap<>();
		String watermarkPath = "watermark.pdf";
		
		for(Map.Entry<Integer, Boolean> entry : map.entrySet()) {
			if (!entry.getValue().booleanValue()) {
				overlayGuide.put(entry.getKey(), watermarkPath);
			}
		}
		
		return overlayGuide;
	}
	
	
	/**
	 * Takes a list of 1-based pages indexes, 
	 * and the number of pages of the document 
	 * and produces a sorted map that has a boolean if the number is contained on the list or not. 
	 * @return
	 * Map (Integer, Boolean)
	 */
	public static Map<Integer, Boolean> convertArrayListToSortedMap(List<Integer> arrayList, int fixedSize) {
        Map<Integer, Boolean> resultMap = new HashMap<>();

        // Initialize the map with all values set to false
        for (int i = 1; i <= fixedSize; i++) {
            resultMap.put(i, false);
        }

        // Update the values to true if they exist in the ArrayList
        for (Integer key : arrayList) {
            if (key >= 1 && key <= fixedSize) {
                resultMap.put(key, true);
            }
        }
        //sorting the map
        return new TreeMap<>(resultMap);
    }
	
	public static boolean isEmpty(Path path) throws IOException {
	    if (Files.isDirectory(path)) {
	        try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
	            return !directory.iterator().hasNext();
	        }
	    }

	    return false;
	}
}
