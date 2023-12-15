package tabula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

		
		String[] extentions = {"pdf"};
		String basePath = "C:\\Users\\George\\Desktop\\pdf";
		Iterator<File> it = FileUtils.iterateFiles(new File(basePath), extentions, false);
		
		File extractedFolder = new File(basePath+"\\extracted");
	    if (extractedFolder.exists())
	    	throw new IOException("\"extracted\" folder already exists");
	    else
	    	extractedFolder.mkdirs();
	    
	    NurminenDetectionAlgorithm nurminenAlgorithm = new NurminenDetectionAlgorithm();
		SpreadsheetDetectionAlgorithm spreadAlgorithm = new SpreadsheetDetectionAlgorithm();
		
        while(it.hasNext()){
        	
            File pdfFile = it.next();
        	System.out.println(pdfFile.getName());
        	InputStream in = new FileInputStream(pdfFile);

        	try (PDDocument document = PDDocument.load(in)) {
        		long start = System.currentTimeMillis();
    			
    			
    			try (ObjectExtractor oe = new ObjectExtractor(document)){
	    		    PageIterator pageIterator = oe.extract();
	    		    
	    		    List<Integer> pagesWithTables = new ArrayList<>(); 
	    		    
	    		    int i=1;
	    		    while (pageIterator.hasNext()) {
	
	    		        Page page = pageIterator.next();
	    		        
	    		        System.out.println(ANSI_YELLOW+"Page: "+ (i) +ANSI_RESET);
	    		        
	    		      //discard empty pages
	    		        if(!document.getPage(i-1).hasContents()) {
	    		        	i++;
	    		        	continue;
	    		        }
	    		        
	    		        // add scanned pages, !page.hasText() produces the same result but is deprecated
	    		        if (page.getText().isEmpty()) {
	    		            pagesWithTables.add(i);
	    		            System.out.println(ANSI_GREEN + "Page: " + i + " is scanned!!!!!!" + ANSI_RESET);
	    		        } 
	    		        
	    		        // spreadAlgorithm is evaluated first because it gives the best results and is faster. the other has some edge cases
	    		        else if (!spreadAlgorithm.detect(page).isEmpty() || !nurminenAlgorithm.detect(page).isEmpty()) {
	    		            pagesWithTables.add(i);
	    		        }
	    		        i++;
	    		    }
	    		    long finish = System.currentTimeMillis();
	    		    long timeElapsed = finish - start;
	    		    
	    		    System.out.println("time elapsed for file: "+ timeElapsed/(double)1000 + " s");
	
	    		    Map<Integer, Boolean> map = convertArrayListToSortedMap(pagesWithTables, document.getNumberOfPages());

	    		    map.forEach((k,v) -> System.out.println("pageNumber: "+(k)+" hasTable: "+v));
	    		    
	    		    String extractedPdfPath = basePath+"\\extracted\\"+FilenameUtils.getBaseName(pdfFile.getName())+"_extracted.pdf";
	    		    
	    		    createPDFWithTablesAndScannedPages(document, map, extractedPdfPath);

    			
    			}
	        }
        }
	}

	public static void createPDFWithTablesAndScannedPages(PDDocument document, Map<Integer, Boolean> map,
			String extractedPdfPath) throws IOException {
		File extractedFile = new File(extractedPdfPath);
		
		System.out.println(extractedFile.getPath());
		
		try (PDDocument extracted = new PDDocument()) {

			map.forEach((pageNumber, hasTables) -> {
				if (hasTables) {
					extracted.addPage(document.getPage(pageNumber - 1));
				} else {
					PDRectangle dimensions = document.getPage(pageNumber - 1).getMediaBox();
					extracted.addPage(new PDPage(dimensions));
				}
			});

			extracted.save(extractedFile);
		}
	}
	
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
