package tabula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

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
		Iterator<File> it = FileUtils.iterateFiles(new File("C:\\Users\\George\\Desktop\\p"), extentions, false);
		
        while(it.hasNext()){
        	
            File pdfFile = it.next();
        	System.out.println(pdfFile.getName());
        	InputStream in = new FileInputStream(pdfFile);
        	
        	List<Integer> pages = new ArrayList<>();
        	
        	try (PDDocument document = PDDocument.load(in)) {
    			
    			BasicExtractionAlgorithm sea = new BasicExtractionAlgorithm();
    			NurminenDetectionAlgorithm nurminenAlgorithm = new NurminenDetectionAlgorithm();
    			SpreadsheetDetectionAlgorithm spreadAlgorithm = new SpreadsheetDetectionAlgorithm();
    		    PageIterator pageIterator = new ObjectExtractor(document).extract();
    		    
    		    
    		    
    		    int k=1;
    		    while (pageIterator.hasNext()) {

    		        Page page = pageIterator.next();
    		        
    		        System.out.println(ANSI_YELLOW+"Page: "+ k +ANSI_RESET);
    		        
    		        List<Rectangle> tablesNurm = nurminenAlgorithm.detect(page);
    		        List<Rectangle> tablesSpread = spreadAlgorithm.detect(page);
    		        
    		        if(tablesNurm.size()!=0&&tablesSpread.size()!=0)
    		        	pages.add(k);
    		        
    		        k++;
    		    }
    		}
        	pages.forEach(p->System.out.println(p));
        }
	}
}
