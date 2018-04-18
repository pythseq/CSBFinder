package Main;

import SuffixTrees.Edge;
import SuffixTrees.InstanceNode;
import Utils.*;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import Main.CommandLineArgs.OutputType;

/**
 * Created by Dina on 19/05/2017.
 * Writes the output files:
 *      catalog_file: OGMs catalog
 *      instances_file: The strings in which each OGM has an instance
 */
public class Writer {
    //output files
    private PrintWriter catalog_file;
    private PrintWriter instances_file;

    private SXSSFWorkbook catalog_workbook;
    private Sheet catalog_sheet;
    private Sheet filtered_patterns_sheet;
    private Sheet patterns_description_sheet;
    String catalog_path;

    private DecimalFormat DF;

    private int max_error;
    private int max_deletion;
    private int max_insertion;
    private int count_printed_patterns;
    private int count_printed_filtered_patterns;
    private int next_line_index_OGB_desc_sheet;
    private boolean cog_info_exists;
    private boolean debug;

    OutputType output_file_type;


    public Writer(int max_error, int max_deletion, int max_insertion, boolean debug, String catalog_path,
                  String instances_path, boolean include_families, OutputType output_file_type,
                  boolean cog_info_exists){
        DF = new DecimalFormat("#.####");
        DF.setRoundingMode(RoundingMode.HALF_UP);

        this.max_error = max_error;
        this.max_deletion = max_deletion;
        this.max_insertion = max_insertion;
        this.debug = debug;
        count_printed_patterns = 0;
        count_printed_filtered_patterns = 0;
        next_line_index_OGB_desc_sheet = 0;

        this.output_file_type = output_file_type;
        this.cog_info_exists = cog_info_exists;

        catalog_sheet = null;
        filtered_patterns_sheet = null;
        patterns_description_sheet = null;

        this.catalog_path = catalog_path;
        createOutputDirectory();
        createFiles(catalog_path, instances_path, include_families);

    }

    private void createOutputDirectory(){
        try {
            new File("output").mkdir();
        }catch (SecurityException e){
            System.out.println("The directory 'output' could not be created, therefore no output is printed. " +
                    "Please create a directory named 'output' in the following path: " + System.getProperty("user.dir"));
        }
    }

    private void createFiles(String catalog_path, String instances_path, boolean include_families){
        String header = "ID\tLength\tScore\tInstance_Count\tInstance_Ratio\tExact_Instance_Count\tOGB";
        if (cog_info_exists){
            header += "\tMain_Category";
        }
        if (include_families){
            header += "\tFamily_ID";
        }

        if (output_file_type == OutputType.TXT) {
            catalog_file = createOutputFile(catalog_path);

            if (catalog_file != null) {
                catalog_file.write(header + "\n");
            }
        }else if(output_file_type == OutputType.XLSX) {
            catalog_workbook = new SXSSFWorkbook(10);
            catalog_sheet = catalog_workbook.createSheet("Catalog");
            writeHeaderToSheet(header, catalog_sheet, include_families);
            if (include_families) {
                filtered_patterns_sheet = catalog_workbook.createSheet("Filtered OGBs");
            }
            if (cog_info_exists){
                patterns_description_sheet = catalog_workbook.createSheet("OGBs description");
            }
            writeHeaderToSheet(header, filtered_patterns_sheet, include_families);
        }

        instances_file = createOutputFile(instances_path);


    }

    private void writeHeaderToSheet(String header, Sheet sheet, boolean include_families){
        Row row = sheet.createRow(0);
        int i = 0;
        for (String str: header.split("\t")){
            row.createCell(i++).setCellValue(str);
        }

    }

    public int getCountPrintedPatterns(){
        return count_printed_patterns;
    }

    public void closeFiles(){

        if (output_file_type == OutputType.TXT) {
            if (catalog_file != null) {
                catalog_file.close();
            }
        }else if(output_file_type == OutputType.XLSX){
            FileOutputStream fileOut = null;
            try {
                fileOut = new FileOutputStream(catalog_path+".xlsx");
                catalog_workbook.write(fileOut);
                fileOut.close();
            } catch (Exception e) {
                System.out.println("A problem occurred while trying to write to file "+catalog_path+".xlsx");
            }
        }
        if (instances_file != null) {
            instances_file.close();
        }
    }

    private void printInstances(Pattern pattern, Utils utils){
        if (instances_file != null) {
            instances_file.println(">" + pattern.getPatternId() + "\t" + pattern.getPattern());

            HashMap<String, ArrayList<String>> instance_seq_and_location = new HashMap<>();
            for (Instance instance : pattern.get_instances()) {
                InstanceNode instance_node = instance.getNodeInstance();
                if (instance.getEdge() != null) {
                    Edge edge = instance.getEdge();
                    instance_node = (InstanceNode) edge.getDest();
                }
                int instance_length = instance.getLength();
                for (Map.Entry<Integer, ArrayList<String>> entry : instance_node.getResults().entrySet()) {
                    String seq_name = utils.genome_key_to_name.get(entry.getKey());

                    if (!instance_seq_and_location.containsKey(seq_name)) {
                        instance_seq_and_location.put(seq_name, new ArrayList<>());
                    }
                    ArrayList<String> word_ids = instance_seq_and_location.get(seq_name);
                    for (String word_id : entry.getValue()) {
                        word_ids.add(word_id+ "_length_" + instance_length);
                    }
                }
            }

            for (Map.Entry<String, ArrayList<String>> entry : instance_seq_and_location.entrySet()) {
                String seq_key = entry.getKey();
                if (seq_key == null){
                    System.out.println("null");
                }
                instances_file.print(seq_key);
                ArrayList<String> word_ids = entry.getValue();
                for (String word_id : word_ids){
                    instances_file.print("\t" + word_id);
                }
                //String word_id = entry.getValue();
                //instances_file.println("seq" + seq + "_" + word_id + "\t");
                //instances_file.println(seq_name);
                instances_file.print("\n");
            }

        }
    }

    /**
     * Prints a pattern with the highest score in its family to a different sheet
     * @param pattern
     * @param utils
     * @param family_id
     */
    public void printFilteredOGB(Pattern pattern, Utils utils, String family_id){
        if(output_file_type == OutputType.XLSX){
            if (pattern != null) {
                count_printed_filtered_patterns++;
                printOGBLineToExcelSheet(filtered_patterns_sheet, pattern, count_printed_filtered_patterns, family_id, utils);
            }
        }
    }

    private void printOGBLineToExcelSheet(Sheet sheet, Pattern pattern, int row_num, String family_id, Utils utils){
        Row row = sheet.createRow(row_num);
        int col = 0;
        row.createCell(col++).setCellValue(pattern.getPatternId());
        row.createCell(col++).setCellValue(pattern.getLength());
        try{
            row.createCell(col++).setCellValue(Double.valueOf(DF.format(pattern.getScore())));
        }catch (Exception e){
            row.createCell(col++).setCellValue(DF.format(pattern.getScore()));
        }
        row.createCell(col++).setCellValue(pattern.getInstanceCount());
        row.createCell(col++).setCellValue(Double.valueOf(DF.format(pattern.getInstanceCount() /
                (double) utils.number_of_genomes)));
        row.createCell(col++).setCellValue(pattern.get_exact_instance_count());
        row.createCell(col++).setCellValue(String.join("-", pattern.getPatternArr()));
        if (utils.cog_info != null) {
            row.createCell(col++).setCellValue(pattern.getMain_functional_category());
        }
        if (family_id != null){
            row.createCell(col++).setCellValue(family_id);
        }
    }

    private int printPatternDescToExcelSheet(Sheet sheet, int row_num, Pattern pattern, Utils utils){
        Row row = sheet.createRow(row_num++);
        row.createCell(0).setCellValue("ID=");
        row.createCell(1).setCellValue(pattern.getPatternId());
        row.createCell(2).setCellValue("Count=");
        row.createCell(3).setCellValue(pattern.getInstanceCount());
        row.createCell(4).setCellValue("Score=");
        row.createCell(5).setCellValue(pattern.getScore());

        for (String cog : pattern.getPatternArr()){
            row = sheet.createRow(row_num++);
            COG cog_obj = utils.cog_info.get(cog);

            row.createCell(0).setCellValue(cog);
            if (cog_obj!=null) {
                row.createCell(1).setCellValue(cog_obj.getCog_desc());
            }else{
                System.out.println(cog + " description is null");
            }
        }
        return row_num+1;
    }

    public void printPattern(Pattern pattern, Utils utils, String family_id){
        if (pattern != null) {
            count_printed_patterns++;
            if(output_file_type == OutputType.XLSX) {
                printOGBLineToExcelSheet(catalog_sheet, pattern, count_printed_patterns, family_id, utils);
                if (patterns_description_sheet != null) {
                    next_line_index_OGB_desc_sheet = printPatternDescToExcelSheet(patterns_description_sheet,
                            next_line_index_OGB_desc_sheet, pattern, utils);
                }
            }else if(output_file_type == OutputType.TXT){
                String catalog_line = pattern.getPatternId() + "\t" + pattern.getLength() + "\t";

                catalog_line += DF.format(pattern.getScore()) + "\t"
                        + pattern.getInstanceCount() + "\t"
                        + DF.format(pattern.getInstanceCount() / (double) utils.number_of_genomes) + "\t"
                        + pattern.get_exact_instance_count() + "\t";

                catalog_line += String.join("-", pattern.getPatternArr()) + "\t";

                if (cog_info_exists) {
                    catalog_line += pattern.getMain_functional_category() + "\t";
                }
                if (family_id != null){
                    catalog_line += family_id;
                }

                if (catalog_file != null) {
                    catalog_file.write(catalog_line + "\n");
                }
            }
            printInstances(pattern, utils);

        }
    }

    public void printPattern(Pattern pattern, Utils utils){
        printPattern(pattern, utils, null);
    }

    private PrintWriter createOutputFile(String path){

        try {
            new File("output").mkdir();
            try {
                PrintWriter output_file = new PrintWriter(path + ".fasta", "UTF-8");
                return output_file;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }catch (SecurityException e){
            System.out.println("The directory 'output' could not be created, therefore no output is printed. " +
                    "Please create a directory named 'output' in the following path: " + System.getProperty("user.dir"));
        }
        return null;
    }


}