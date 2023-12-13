package tabula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.pdmodel.PDDocument;

import technology.tabula.CommandLineApp;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.*;



public class Main {
	
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_CYAN = "\u001B[36m";

	public static void main(String[] args) throws IOException, ParseException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException, IllegalArgumentException, ClassNotFoundException {

		File initialFile = new File("c:\\Users\\George\\Desktop\\p\\aa_2458990s.pdf");
	    InputStream in = new FileInputStream(initialFile);
	    
	   
	    
		try (PDDocument document = PDDocument.load(in)) {
			
			BasicExtractionAlgorithm sea = new BasicExtractionAlgorithm();
			
			
		    PageIterator pageIterator = new ObjectExtractor(document).extract();
		    int k=1;
		    while (pageIterator.hasNext()) {
		        // iterate over the pages of the document
		        Page page = pageIterator.next();
		        
		        System.out.println(ANSI_YELLOW+"Page: "+k+ANSI_RESET);
		        List<Table> pageTables =null;
		        try {
		        	
		        	
		        	
		        	
		        	
		        	
		        	Class<?> privateInnerClass = Class.forName("technology.tabula.CommandLineApp$TableExtractor");
			        
		        	Constructor<?> constructor = privateInnerClass.getDeclaredConstructor();
		        	constructor.setAccessible(true);
			        Object tableExtractorConstructor = constructor.newInstance();
			        
			        Field guessField  = privateInnerClass.getDeclaredField("guess");
			        Field methodField  = privateInnerClass.getDeclaredField("method");
			        guessField.setAccessible(true);
			        methodField.setAccessible(true);
			        
//			        Class<?> privateEnumClass = Class.forName("technology.tabula.CommandLineApp$TableExtractor$ExtractionMethod");
			        
			        
//			        Field enumField = privateEnumClass.getDeclaredField("DECIDE");
//			        Object enumConstantValue = enumField.get(null);

			        guessField.set(tableExtractorConstructor,true);
//			        methodField.set(tableExtractorConstructor, enumConstantValue);
			        
			        Method extractTables = privateInnerClass.getDeclaredMethod("extractTables", Page.class);
			        extractTables.setAccessible(true);

			        pageTables = (List<Table>) extractTables.invoke(tableExtractorConstructor, page);
			        System.out.println(pageTables.size()+ " tables----------------------");
		        }
		        catch (Exception e) {
					e.printStackTrace();
				}
		        
//		        pageTables = sea.extract(page);
		        // iterate over the tables of the page
		        int j=1;
		        for(Table tableT: pageTables) {
		        	String[][] tablea = new String[tableT.getRowCount()][tableT.getColCount()];
		        	System.out.println(ANSI_GREEN+"Table: "+j+ANSI_RESET);
//		        	System.out.print(table+"--> ");
		            List<List<RectangularTextContainer>> rows = tableT.getRows();
//		             iterate over the rows of the table
		            int m=0;
		            for (List<RectangularTextContainer> cells : rows) {
//		                // print all column-cells of the row plus linefeed
		            	int n=0;
		                for (RectangularTextContainer content : cells) {
//		                    // Note: Cell.getText() uses \r to concat text chunks
		                    String text = content.getText().replace("\r", "");
		                    tablea[m][n]=text;
		                    n++;
		                }
		                
		                m++;
		            }
		            j++;
		            /*
		        	 * leftJustifiedRows - If true, it will add "-" as a flag to format string to
		        	 * make it left justified. Otherwise right justified.
		        	 */
		        	boolean leftJustifiedRows = true;
		         
		        	/*
		        	 * Table to print in console in 2-dimensional array. Each sub-array is a row.
		        	 */
		        	String[][] table = tablea;
		         
		        	/*
		        	 * Calculate appropriate Length of each column by looking at width of data in
		        	 * each column.
		        	 * 
		        	 * Map columnLengths is <column_number, column_length>
		        	 */
		        	Map<Integer, Integer> columnLengths = new HashMap<>();
		        	Arrays.stream(table).forEach(a -> Stream.iterate(0, (i -> i < a.length), (i -> ++i)).forEach(i -> {
		        		if (columnLengths.get(i) == null) {
		        			columnLengths.put(i, 0);
		        		}
		        		if (columnLengths.get(i) < a[i].length()) {
		        			columnLengths.put(i, a[i].length());
		        		}
		        	}));
//		        	System.out.println("columnLengths = " + columnLengths);
		         
		        	/*
		        	 * Prepare format String
		        	 */
		        	final StringBuilder formatString = new StringBuilder("");
		        	String flag = leftJustifiedRows ? "-" : "";
		        	columnLengths.entrySet().stream().forEach(e -> formatString.append("| %" + flag + (e.getValue()==0?1:e.getValue()) + "s "));
		        	formatString.append("|\n");
//		        	System.out.println("formatString = " + formatString.toString());
		         
		        	/*
		        	 * Print table
		        	 */
		        	
		        		Stream.iterate(0, (i -> i < table.length), (i -> ++i))
	        			.forEach(a -> System.out.printf(formatString.toString(), table[a]));
		        	
		        }
		        k++;
		    }
		}

	}

}
