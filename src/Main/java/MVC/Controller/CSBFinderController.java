package MVC.Controller;

import MVC.Common.*;
import MVC.Model.CSBFinderModel;
import MVC.View.MainFrame;
import Utils.COG;
import Utils.Pattern;
import Utils.Gene;

import java.util.List;
import java.util.Map;

public class CSBFinderController {

    private CSBFinderModel model;
    private MainFrame view;

    public CSBFinderController() {
        this.model = new CSBFinderModel();
        this.view = new MainFrame(this);

//        this.model.setGenomesLoadedListener(new GenomesLoadedListener() {
//            @Override
//            public void genomesLoadDone(GenomesLoadEvent e) {
//                view.displayInputPanel(model.getNumberOfGenomes());
//            }
//        });

        this.model.setCSBFinderDoneListener(new CSBFinderDoneListener() {
            @Override
            public void CSBFinderDoneOccurred(CSBFinderDoneEvent e) {
                view.displayFamilyTable(e.getFamilyList());
            }
        });
    }

    public void loadInputGenomesFile(String file_path) {
        this.model.loadInputGenomesFile(file_path);
    }

    public void saveOutputFiles(String outputFileType) { this.model.saveOutputFiles(outputFileType); }

    public void findCSBs(CSBFinderRequest request) {
        this.model.findCSBs(request);
    }

    public List<COG> getCogInfo(List<String> cogs) {
        return model.getCogInfo(cogs);
    }

    public Map<String, List<List<Gene>>> getInstances(Pattern pattern) { return model.getInstances(pattern); }

    public int getGenomesLoaded() {
        return model.getNumberOfGenomes();
    }

//    public static void main(String[] args) {
//        CSBFinderController controller = new CSBFinderController();
//        controller.loadInputGenomesFile("E:\\Coding\\java\\CSBFinderCore\\input\\plasmid_genomes.fasta", true);
//
//        CSBFinderRequest request = new CSBFinderRequest();
////        request.setCsb_pattern_file_name("e:\\Coding\\java\\CSBFinderCore\\input\\cog_info.txt");
//        controller.findCSBs(request);
//
//
//    }


}