package tabula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.detectors.SpreadsheetDetectionAlgorithm;
import technology.tabula.extractors.*;



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
		
        while(it.hasNext()){
        	
            File pdfFile = it.next();
        	System.out.println(pdfFile.getName());
        	InputStream in = new FileInputStream(pdfFile);

        	try (PDDocument document = PDDocument.load(in)) {
        		long start = System.currentTimeMillis();
    			NurminenDetectionAlgorithm nurminenAlgorithm = new NurminenDetectionAlgorithm();
    			SpreadsheetDetectionAlgorithm spreadAlgorithm = new SpreadsheetDetectionAlgorithm();
    			
    			try (ObjectExtractor oe = new ObjectExtractor(document)){
	    		    PageIterator pageIterator = oe.extract();
	    		    
	    		    List<Integer> pagesWithTables = new ArrayList<>(); 
	    		    
	    		    int i=1;
	    		    while (pageIterator.hasNext()) {
	
	    		        Page page = pageIterator.next();
	    		        
	    		        System.out.println(ANSI_YELLOW+"Page: "+ (i) +ANSI_RESET);
	    		        
	    		        
	    		        List<Rectangle> tablesSpread = spreadAlgorithm.detect(page);
	    		        
	    		        if(!tablesSpread.isEmpty())
	    		        	pagesWithTables.add(i);
	    		        else {
	    		        	List<Rectangle> tablesNurm = nurminenAlgorithm.detect(page);
	    		        	if (!tablesNurm.isEmpty())
	    		        		pagesWithTables.add(i);
	    		        	else {}
	    		        }
	    		        i++;
	    		    }
	    		    long finish = System.currentTimeMillis();
	    		    long timeElapsed = finish - start;
	
	    		    Map<Integer, Boolean> map = convertArrayListToSortedMap(pagesWithTables, document.getNumberOfPages());

	    		    System.out.println("time elapsed: "+ timeElapsed/(double)1000 + " s");
	    		    
	    		    map.forEach((k,v) -> System.out.println("pageNumber: "+(k)+" hasTable: "+v));
	    		    
	    		    String extractedPdfPath = basePath+"\\extracted\\"+FilenameUtils.getBaseName(pdfFile.getName())+"_extracted.pdf";
	    		    File extractedFile = new File(extractedPdfPath);
	    		    
	    		    System.out.println(extractedFile.getPath());
	    		    PDDocument extracted = new PDDocument();
	    		    
	    		    
	    		    map.forEach((pageNumber,hasTables) -> {
	    		    	if(hasTables) {
	    		    		extracted.addPage(document.getPage(pageNumber-1));
	    		    	}
	    		    	else {
	    		    		PDRectangle dimensions = document.getPage(pageNumber-1).getMediaBox();
	    		    		extracted.addPage(new PDPage(dimensions));
						}
	    		    });
	    		    
	    		    extracted.save(extractedFile);
	    		    extracted.close();
	    		    
	    		    
	    		    
	    		    
	    		    System.out.println(document.getDocumentInformation().getCOSObject());
    			}
    			
    			
    			 
	        }
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
