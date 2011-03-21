/**
 *  neuroConstruct
 *  Software for developing large scale 3D networks of biologically realistic neurons
 * 
 *  Copyright (c) 2009 Padraig Gleeson
 *  UCL Department of Neuroscience, Physiology and Pharmacology
 *
 *  Development of this software was made possible with funding from the
 *  Medical Research Council and the Wellcome Trust
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package ucl.physiol.neuroconstruct.project;

import ucl.physiol.neuroconstruct.utils.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;
import ucl.physiol.neuroconstruct.cell.*;
import ucl.physiol.neuroconstruct.cell.utils.CellTopologyHelper;
import ucl.physiol.neuroconstruct.mechanisms.CellMechanism;
import ucl.physiol.neuroconstruct.mechanisms.ChannelMLCellMechanism;
import ucl.physiol.neuroconstruct.mechanisms.XMLMechanismException;
import ucl.physiol.neuroconstruct.project.packing.CellPackingAdapter;

/**
 * Class for generating HTML representation of neuroConstruct project
 *
 * @author Padraig Gleeson
 *  
 */
public class Expand {

    private static ClassLogger logger = new ClassLogger("Expand");
    static String CELL_TYPES = "cellTypes";
    static String CELL_MECHANISMS = "cellMechanisms";
    private static int fontSize = 10;
    public static final String COLOUR_AMPA = "#FF0000";
    public static final String COLOUR_NMDA = "#FF9900";
    public static final String COLOUR_GABA = "#0000FF";
    public static final String COLOUR_GAP = "#669966";

    public Expand() {
    }

    public static void generateProjectView(Project project, File dir) {
    }

    public static String getItemPage(String origName) {
        return GeneralUtils.replaceAllTokens(origName, " ", "_") + ".html";
    }

    public static String getCellTypePage(String cellTypeName) {
        return CELL_TYPES + "/" + getItemPage(cellTypeName);
    }

    public static String getCellMechPage(String cellMechName) {
        return CELL_MECHANISMS + "/" + getItemPage(cellMechName);
    }

    public static String getNetConnInfo(Project project, String nc) {
        StringBuilder sb = new StringBuilder();
        //String src = project.morphNetworkConnectionsInfo.getSourceCellGroup(nc);
        //String tgt = project.morphNetworkConnectionsInfo.getTargetCellGroup(nc);

        if (project.morphNetworkConnectionsInfo.isValidSimpleNetConn(nc)) {
            ConnectivityConditions cc = project.morphNetworkConnectionsInfo.getConnectivityConditions(nc);

            NumberGenerator start = cc.getNumConnsInitiatingCellGroup();
            int maxFin = cc.getMaxNumInitPerFinishCell();

            if (cc.getGenerationDirection() == ConnectivityConditions.SOURCE_TO_TARGET) {
                String max = maxFin == Integer.MAX_VALUE ? "X" : "max " + maxFin;
                sb.append(start.toShortString() + " -> " + max);
            } else if (cc.getGenerationDirection() == ConnectivityConditions.TARGET_TO_SOURCE) {
                String max = maxFin == Integer.MAX_VALUE ? "X" : "max " + maxFin;
                sb.append(max + " -> " + start.toShortString());
            }
            Vector<SynapticProperties> syns = project.morphNetworkConnectionsInfo.getSynapseList(nc);

            sb.append("<br/>");
            for (SynapticProperties syn : syns) {
                String synRef = syn.getSynapseType();
                if (synRef.length() > 12) {
                    if (synRef.indexOf("AMPA") >= 0) {
                        synRef = "AMPA ";
                    } else if (synRef.indexOf("NMDA") >= 0) {
                        synRef = "NMDA ";
                    } else if (synRef.indexOf("GABAA") >= 0) {
                        synRef = "GABAA ";
                    } else if (synRef.indexOf("GABA") >= 0) {
                        synRef = "GABA ";
                    } else if (synRef.indexOf("Elect") >= 0) {
                        synRef = "Gap J ";
                    } else if (synRef.indexOf("Gap") >= 0) {
                        synRef = "Gap J ";
                    } else {
                        synRef = "syn ";
                    }
                }
                sb.append("<a href = \"" + getCellMechPage(syn.getSynapseType()) + "\">" + synRef + "</a> ");

            }

            //sb.append(")");
        } else {
            ConnectivityConditions cc = project.volBasedConnsInfo.getConnectivityConditions(nc);
            sb.append(cc.toString());
        }

        return sb.toString();
    }

    public static void generateModelDescriptions(ArrayList<String> projPaths, String dirName) {

        logger.logComment("Going to create documentation at " + dirName + " for " + projPaths, true);

        File descriptionsDir = new File(dirName);
        ArrayList<Project> projects = new ArrayList<Project>();

        String indexTitle = "index.html";
        GeneralUtils.removeAllFiles(descriptionsDir, false, false, true);
        SimpleHtmlDoc indexPage = new SimpleHtmlDoc("Model descriptions", fontSize);
        File fileToSave = new File(dirName, indexTitle);

        indexPage.addTaggedElement("neuroConstruct project descriptions", "h1");
        indexPage.addRawHtml("This is a collection of automatically-generated descriptions for neuroConstruct projects.");
        indexPage.addRawHtml("<dl>");

        for (String projPath : projPaths) {
            try {
                File projFile = new File(projPath);
                Project project = Project.loadProject(projFile, new ProjectEventListener() {

                    public void tableDataModelUpdated(String tableModelName) {
                    }

                    ;

                    public void tabUpdated(String tabName) {
                    }

                    ;

                    public void cellMechanismUpdated() {
                    }

                    ;
                });
                projects.add(project);

                String projName = project.getProjectName();
                String projNameStripped = GeneralUtils.replaceAllTokens(projName, " ", "_");
                File projSpecificDir = new File(descriptionsDir, projNameStripped);
                if (!projSpecificDir.exists()) {
                    projSpecificDir.mkdir();
                }

                File f = generateMainPage(project, projSpecificDir);
                File projRelativePath = new File(projNameStripped, f.getName());

                indexPage.addTaggedElement(indexPage.getLinkedText(projName, projRelativePath.toString()), "dt");
                indexPage.addTaggedElement(project.getProjectDescription(), "dd");

                /*File smallImg = new File(projFile.getParentFile(), "images/small.png");
                if (smallImg.exists()) {
                    File smallImgInDoc = new File(projSpecificDir, "images");
                    File smallImgInDocRelative = new File(projNameStripped, "images/small.png");
                    if (!smallImgInDoc.exists()) {
                        smallImgInDoc.mkdir();
                    }
                    GeneralUtils.copyFileIntoDir(smallImg, smallImgInDoc);
                    indexPage.addRawHtml("<img src=\"" + smallImgInDocRelative.toString() + "\" align=\"right\">");
                } else {
                    System.out.println("Project image not found at " + smallImg.toString());
                }*/

                logger.logComment("Created a doc at: " + f.getCanonicalPath(), true);


            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        indexPage.addRawHtml("</dl>");
        indexPage.saveAsFile(fileToSave);



    }

    public static File generateMainPage(Project project, File dirToCreateIn) {
        String mainPageTitle = project.getProjectFileName();

        ArrayList<String> pages = new ArrayList<String>();
        pages.add(mainPageTitle);

        GeneralUtils.removeAllFiles(dirToCreateIn, false, false, true);

        for (String sc : project.simConfigInfo.getAllSimConfigNames()) {
            pages.add(sc);
        }

        for (String title : pages) {
            File fileToSave = new File(dirToCreateIn, getItemPage(title));

            SimpleHtmlDoc mainPage = new SimpleHtmlDoc(project.getProjectName() + ": " + title, fontSize);

            mainPage.addTaggedElement("neuroConstruct project: " + project.getProjectName(), "h2");

            mainPage.addTaggedElement("Simulation Configurations", "h2");


            for (String sc : project.simConfigInfo.getAllSimConfigNames()) {
                String scFile = getItemPage(sc);
                mainPage.addTaggedElement(mainPage.getLinkedText(sc, scFile), "b");
            }

            String desc = project.getProjectDescription();

            ArrayList<Cell> cells = new ArrayList<Cell>();

            ArrayList<String> cellMechs = new ArrayList<String>();
            ArrayList<String> cellGroups = new ArrayList<String>();
            ArrayList<String> netConns = new ArrayList<String>();
            ArrayList<String> inputs = new ArrayList<String>();

            if (title.equals(mainPageTitle)) {
                cells.addAll(project.cellManager.getAllCells());

                cellMechs.addAll(project.cellMechanismInfo.getAllCellMechanismNames());

                cellGroups = project.cellGroupsInfo.getAllCellGroupNames();

                netConns.addAll(project.morphNetworkConnectionsInfo.getAllSimpleNetConnNames());
                netConns.addAll(project.volBasedConnsInfo.getAllAAConnNames());

                inputs.addAll(project.elecInputInfo.getAllStimRefs());
            } else {
                SimConfig sc = project.simConfigInfo.getSimConfig(title);
                desc = sc.getDescription();
                //cells.removeAllElements();
                //cellMechs.removeAllElements();

                cellGroups = sc.getCellGroups();
                netConns = sc.getNetConns();
                inputs = sc.getInputs();

                for (String cg : sc.getCellGroups()) {
                    String cellType = project.cellGroupsInfo.getCellType(cg);
                    Cell cell = project.cellManager.getCell(cellType);

                    if (!cells.contains(cell)) {
                        cells.add(cell);
                    }

                    ArrayList<String> cms = cell.getAllChanMechNames(true);

                    cms.addAll(cell.getAllAllowedSynapseTypes());
                    for (String cm : cms) {
                        if (!cellMechs.contains(cm)) {
                            cellMechs.add(cm);
                        }
                    }

                }
            }

            mainPage.addTaggedElement("Connectivity Matrix", "h2");

            String connTitle = "Connectivity in " + title;
            if (title.equals(mainPageTitle)) {
                connTitle = "Connectivity in " + SimConfigInfo.DEFAULT_SIM_CONFIG_NAME;
            }


            ArrayList<String> orderedCellGroups = new ArrayList<String>();
            String gap = "_____";

            for (String cg : cellGroups) {
                Region reg = project.regionsInfo.getRegionObject(project.cellGroupsInfo.getRegionName(cg));
                orderedCellGroups.add((reg.getHighestYValue() + 100000) + gap + cg);
            }

            orderedCellGroups = (ArrayList<String>) GeneralUtils.reorderAlphabetically(orderedCellGroups, false);

            for (int i = 0; i < orderedCellGroups.size(); i++) {
                String old = orderedCellGroups.get(i);
                orderedCellGroups.set(i, old.substring(old.indexOf(gap) + gap.length()));
            }


            if (!title.equals(mainPageTitle)) {

                String mFilename = getItemPage(connTitle);
                File mFile = new File(fileToSave.getParentFile(), mFilename);

                mainPage.addTaggedElement(mainPage.getLinkedText(connTitle, mFilename), "b");

                SimpleHtmlDoc matrixPage = new SimpleHtmlDoc(connTitle, fontSize);

                matrixPage.addTaggedElement(connTitle, "h2");


                matrixPage.addRawHtml("<table border=\"1\"  valign='centre' cellpadding='3'>");

                matrixPage.addRawHtml("<tr>");
                matrixPage.addRawHtml("<td   colspan='2'>&nbsp;</td>");

                for (String preCG : orderedCellGroups) {
                    String preRef = preCG;

                    if (preCG.startsWith("CG3D_")) {
                        preRef = preCG.substring(5);
                    }
                    matrixPage.addRawHtml("<td class='header'  colspan='2'>" + preRef + "</td>");
                }
                matrixPage.addRawHtml("</tr>");
                for (String postCG : orderedCellGroups) {
                    matrixPage.addRawHtml("<tr>");
                    String postRef = postCG;

                    if (postCG.startsWith("CG3D_")) {
                        postRef = postCG.substring(5);
                    }
                    matrixPage.addRawHtml("<td class='header'  colspan='2'>" + postRef + "</td>");

                    for (String preCG : orderedCellGroups) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<table>");
                        for (String nc : netConns) {
                            String src = project.morphNetworkConnectionsInfo.getSourceCellGroup(nc);
                            String tgt = project.morphNetworkConnectionsInfo.getTargetCellGroup(nc);
                            if (preCG.equals(src) && postCG.equals(tgt)) {
                                String info = getNetConnInfo(project, nc);
                                String bgCol = "";
                                String fgCol = " style=\"color:#FFFFFF\"";

                                if (info.toUpperCase().indexOf("AMPA") >= 0 || info.toUpperCase().indexOf("NMDA") >= 0 || info.toUpperCase().indexOf("EXC") >= 0 || info.toUpperCase().indexOf("DOUBEXPSYN") >= 0) {
                                    bgCol = "bgcolor=\"" + COLOUR_AMPA + "\" " + fgCol;
                                    //info = "<font color=\""+COLOUR_AMPA+"\">"+info+"</font>";
                                } else if (info.toUpperCase().indexOf("GABA") >= 0 || info.toUpperCase().indexOf("INH") >= 0) {
                                    bgCol = "bgcolor=\"" + COLOUR_GABA + "\" " + fgCol;
                                    //info = "<font color=\""+COLOUR_GABA+"\">"+info+"</font>";
                                } else if (info.toUpperCase().indexOf("GAP") >= 0 || info.indexOf("ELECT") >= 0) {
                                    bgCol = "bgcolor=\"" + COLOUR_GAP + "\" " + fgCol;
                                    //info = "<font color=\""+COLOUR_GAP+"\">"+info+"</font>";
                                }
                                sb.append("<tr><td " + bgCol + ">" + info + "</td></td>");
                            }
                        }

                        sb.append("</table>");
                        matrixPage.addRawHtml("<td   colspan='2'>" + sb.toString() + "</td>");
                    }
                    matrixPage.addRawHtml("</tr>");
                }
                matrixPage.addRawHtml("</table>");



                matrixPage.saveAsFile(mFile);
            }






            int width = 700;
            int width1 = 140;


            mainPage.addRawHtml("<p>&nbsp;</p>");

            mainPage.addRawHtml("<table border=\"1\" width='" + width + "' valign='centre' cellpadding='3'>");

            mainPage.addRawHtml("<tr><td class='header'  colspan='2'><b>A: Model Summary</b></td></tr>");
            mainPage.addRawHtml("<tr><td width='" + width1 + "'><b>Description</b></td><td>" + handleWhitespaces(desc) + "</td></tr>");
            mainPage.addRawHtml("<tr><td><b>Populations</b></td><td>");
            for (String cg : cellGroups) {
                String cellType = project.cellGroupsInfo.getCellType(cg);
                String cellPageLoc = getCellTypePage(cellType);
                mainPage.addRawHtml("<a href=\"#" + cg + "\">" + cg + "</a>");

                if (!cg.equals(cellGroups.get(cellGroups.size() - 1))) {
                    mainPage.addRawHtml(" ");
                }
            }

            mainPage.addRawHtml("</td></tr>");
            mainPage.addRawHtml("<tr><td><b>Topology</b></td><td>Network of neurons positioned & connected in 3D space</td></tr>");

            mainPage.addRawHtml("<tr><td><b>Connectivity</b></td><td>");
            if (netConns.isEmpty()) {
                mainPage.addRawHtml("No network connections in this Simulation Configuration");
            }

            for (String nc : netConns) {
                mainPage.addRawHtml("<a href=\"#" + nc + "\">" + nc + "</a>");
                if (!nc.equals(netConns.get(netConns.size() - 1))) {
                    mainPage.addRawHtml(",");
                }
            }
            mainPage.addRawHtml("</td></tr>");


            mainPage.addRawHtml("<tr><td><b>Neuron models</b></td><td>");
            for (Cell cell : cells) {
                //mainPage.addRawHtml(cell.getInstanceName());

                String cellPageLoc = getCellTypePage(cell.getInstanceName());
                mainPage.addRawHtml(" " + "<a href = \"#" + cell.getInstanceName() + "\">" + cell.getInstanceName() + "</a>");

                if (!cell.equals(cells.get(cells.size() - 1))) {
                    mainPage.addRawHtml(", ");
                }

                SimpleHtmlDoc cellPage = new SimpleHtmlDoc("Cell: " + cell.getInstanceName(), fontSize);

                cellPage.addRawHtml(CellTopologyHelper.printDetails(cell, project, true, true, false, true));

                File cellFile = new File(fileToSave.getParentFile(), cellPageLoc);
                if (!cellFile.exists()) {
                    cellPage.saveAsFile(cellFile);
                }

            }
            mainPage.addRawHtml("</td></tr>");


            StringBuffer cmInfo = new StringBuffer();
            StringBuffer synInfo = new StringBuffer();

            for (String cmName : cellMechs) {
                StringBuffer mechInfo = cmInfo;

                CellMechanism cm = project.cellMechanismInfo.getCellMechanism(cmName);


                if (cm.isSynapticMechanism() || cm.isGapJunctionMechanism()) {
                    mechInfo = synInfo;
                }


                //mechInfo.append(cm+" ");


                String cmPageLoc = getCellMechPage(cm.getInstanceName());

                mechInfo.append("<a href = \"" + cmPageLoc + "\">" + cm.getInstanceName() + "</a> ");

                File xslDoc = GeneralProperties.getChannelMLReadableXSL();


                SimpleHtmlDoc cmPage = new SimpleHtmlDoc("Cell Mechanisms: " + cm.getInstanceName(), fontSize);

                File cmPageFile = new File(fileToSave.getParentFile(), cmPageLoc);

                if (cm instanceof ChannelMLCellMechanism) {
                    ChannelMLCellMechanism cmlCm = (ChannelMLCellMechanism) cm;

                    String cmXmlPageLoc = getCellMechPage(cm.getInstanceName() + ".channelml");
                    File cmXmlPageFile = new File(fileToSave.getParentFile(), cmXmlPageLoc);


                    if (!cmXmlPageFile.exists() || !cmPageFile.exists()) {
                        try {
                            String readable = XMLUtils.transform(cmlCm.getXMLDoc().getXMLString("", false), xslDoc);

                            cmPage.addRawHtml(readable);
                        } catch (XMLMechanismException e) {
                            cmPage.addTaggedElement("Unable to generate HTML representation of: " + cm.getInstanceName(), "b");
                        }

                        SimpleHtmlDoc cmXmlPage = new SimpleHtmlDoc("Cell Mechanism: " + cm.getInstanceName() + " in ChannelML", fontSize);


                        try {
                            String cmlString = cmlCm.getXMLDoc().getXMLString("", true);


                            cmXmlPage.addRawHtml(cmlString);
                        } catch (XMLMechanismException e) {
                            cmXmlPage.addTaggedElement("Unable to generate ChannelML representation of: " + cm.getInstanceName(), "b");
                        }

                        cmXmlPage.saveAsFile(cmXmlPageFile);
                        cmPage.saveAsFile(cmPageFile);

                        //mainPage.addRawHtml("</td><td><a href = "+cmXmlPageLoc	+">ChannelML file</td><td>");
                    }
                } else {
                }



            }

            mainPage.addRawHtml("<tr><td><b>Channel models</b></td><td>" + cmInfo + "</td></tr>");
            if (synInfo.length() == 0) {
                synInfo.append("No synapses present in this Simulation Configuration");
            }
            mainPage.addRawHtml("<tr><td><b>Synapse models</b></td><td>" + synInfo + "</td></tr>");


            mainPage.addRawHtml("<tr><td><b>Input</b></td><td>");
            for (String in : inputs) {
                String cg = project.elecInputInfo.getStim(in).getCellGroup();

                mainPage.addRawHtml(" " + in + " (to <a href=\"#" + cg + "\">" + cg + "</a>)");
            }
            mainPage.addRawHtml("</td></tr>");

            mainPage.addRawHtml("</table>");



            mainPage.addRawHtml("<p>&nbsp;</p>");

            mainPage.addRawHtml("<table border=\"1\" width='" + width + "'  cellpadding='3'>");

            mainPage.addRawHtml("<tr><td class='header'  colspan='3'><b>B: Populations</b></td></tr>");

            mainPage.addRawHtml("<tr><td width='" + width1 + "'><b>Name</b></td>"
                    + "<td  width='100'><b>Elements</b></td>"
                    + "<td><b>Description</b></td></tr>");

            for (String cg : cellGroups) {

                String cellType = project.cellGroupsInfo.getCellType(cg);
                String cellPageLoc = getCellTypePage(cellType);

                String regionName = project.cellGroupsInfo.getRegionName(cg);
                Region region = project.regionsInfo.getRegionObject(regionName);
                CellPackingAdapter cpa = project.cellGroupsInfo.getCellPackingAdapter(cg);

                mainPage.addRawHtml("<tr><td><a name=\"" + cg + "\"/>" + cg + "</td>"
                        + "<td><a href = \"" + cellPageLoc + "\">" + project.cellGroupsInfo.getCellType(cg) + "</a></td>"
                        + "<td>" + cpa.toNiceString() + "<br>"
                        + "In region (" + regionName + "): " + region.toString() + "</td></tr>");
            }


            mainPage.addRawHtml("</table>");

            mainPage.addRawHtml("<p>&nbsp;</p>");

            mainPage.addRawHtml("<table border=\"1\" width='" + width + "' valign='centre' cellpadding='3'>");

            mainPage.addRawHtml("<tr><td class='header'  colspan='4'><b>C: Connectivity</b></td></tr>");


            mainPage.addRawHtml("<tr><td width='" + width1 + "'><b>Name</b></td>"
                    + "<td  width='100'><b>Source</b></td>"
                    + "<td  width='100'><b>Target</b></td>"
                    + "<td><b>Pattern</b></td></tr>");

            if (netConns.isEmpty()) {
                mainPage.addRawHtml("<tr>"
                        + "<td colspan='4'>No network connections in this simulation Configuration</td>"
                        + "</tr>");
            }

            for (String nc : netConns) {
                String src = project.morphNetworkConnectionsInfo.getSourceCellGroup(nc);
                String tgt = project.morphNetworkConnectionsInfo.getTargetCellGroup(nc);
                //ConnectivityConditions cc = project.morphNetworkConnectionsInfo.getConnectivityConditions(nc);

                mainPage.addRawHtml("<tr>"
                        + "<td><a name=\"" + nc + "\"/>" + nc + "</td>"
                        + "<td><a href=\"#" + src + "\">" + src + "</a></td>"
                        + "<td><a href=\"#" + tgt + "\">" + tgt + "</td>"
                        + "<td>" + getNetConnInfo(project, nc) + "</td>"
                        + "</tr>");

            }
            mainPage.addRawHtml("</table>");



            mainPage.addRawHtml("<p>&nbsp;</p>");


            mainPage.addRawHtml("<table border=\"1\" width='" + width + "' valign='centre' cellpadding='3'>");

            mainPage.addRawHtml("<tr><td class='header' colspan='4'>D: Neuron and Synapse models</b></tr>");

            mainPage.addRawHtml("<tr><td width='" + width1 + "'><b>Name</b></td>"
                    + "<td ><b>Description</b></td>"
                    + "<td  width='50'><b>Details</b></td></tr>");

            for (Cell cell : cells) {
                String cellPageLoc = getCellTypePage(cell.getInstanceName());

                mainPage.addRawHtml("<tr>"
                        + "<td><a name=\"" + cell.getInstanceName() + "\"/>" + cell.getInstanceName() + "</td>"
                        + "<td>" + cell.getCellDescription() + "</a></td>"
                        + "<td><a href=\"" + cellPageLoc + "\">More...</td>"
                        + "</tr>");

            }

            mainPage.addRawHtml("</table>");


            mainPage.addRawHtml("<p>&nbsp;</p>");


            mainPage.addRawHtml("<table border=\"1\" width='" + width + "' valign='centre' cellpadding='3'>");

            mainPage.addRawHtml("<tr><td class='header' colspan='4'>E: Inputs</b></tr>");

            mainPage.addRawHtml("<tr><td width='" + width1 + "'><b>Name</b></td>"
                    + "<td ><b>Description</b></td>"
                    + "<td  width='50'><b>Details</b></td></tr>");


            mainPage.addRawHtml("</table>");


            mainPage.addRawHtml("<p>&nbsp;</p>");


            mainPage.addRawHtml("<table border=\"1\" width='" + width + "' valign='centre' cellpadding='3'>");

            mainPage.addRawHtml("<tr><td class='header' colspan='4'>F: Measurements</b></tr>");

            mainPage.addRawHtml("<tr><td width='" + width1 + "'><b>Name</b></td>"
                    + "<td ><b>Description</b></td>"
                    + "<td  width='50'><b>Details</b></td></tr>");


            mainPage.addRawHtml("</table>");


            logger.logComment("Going to save: " + fileToSave.getAbsolutePath(), true);
            mainPage.saveAsFile(fileToSave);
        }

        return new File(dirToCreateIn, getItemPage(mainPageTitle));

    }

    public static String handleWhitespaces(String text) {
        return GeneralUtils.replaceAllTokens(text, "\n", "<br/>");
    }

    public static void main(String[] args) {
        Expand expand = new Expand();

        ArrayList<String> paths = new ArrayList<String>();
        //paths.add("examples/Ex6-Cerebellum/Ex6-Cerebellum.neuro.xml");
        //paths.add("nCmodels/Thalamocortical/Thalamocortical.ncx");
        paths.add("nCmodels/CA1PyramidalCell/CA1PyramidalCell.ncx");
        paths.add("nCmodels/GranuleCell/GranuleCell.ncx");
        paths.add("nCmodels/SolinasEtAl_GolgiCell/SolinasEtAl_GolgiCell.ncx");
        paths.add("nCmodels/GranCellLayer/GranCellLayer.ncx");
        //paths.add("nCexamples/Ex4_HHcell/Ex4_HHcell.ncx");
        //paths.add("/bernal/models/Layer23_names/Layer23_names.neuro.xml");
        //paths.add("../copyNcModels/Parallel/Parallel.neuro.xml");
        //paths.add("nCmodels/Thalamocortical/Thalamocortical.ncx");
        //paths.add("nCexamples/Ex6_CerebellumDemo/Ex6_CerebellumDemo.ncx");
        //paths.add("nCexamples/Ex5_Networks/Ex5_Networks.ncx");
        paths.add("/home/eugenio/phd/code/osb/add_more_models/osbModels/PurkinjeCell/PurkinjeCell.ncx");

        paths = (ArrayList<String>) GeneralUtils.reorderAlphabetically(paths, true);
        generateModelDescriptions(paths, "../temp/testExpand");

    }
}
