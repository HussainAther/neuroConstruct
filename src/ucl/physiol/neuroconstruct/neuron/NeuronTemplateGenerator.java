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

package ucl.physiol.neuroconstruct.neuron;

import java.io.*;
import java.util.*;

import javax.vecmath.*;

import ucl.physiol.neuroconstruct.cell.*;
import ucl.physiol.neuroconstruct.cell.utils.*;
import ucl.physiol.neuroconstruct.mechanisms.*;
import ucl.physiol.neuroconstruct.neuroml.*;
import ucl.physiol.neuroconstruct.project.*;
import ucl.physiol.neuroconstruct.utils.*;
import ucl.physiol.neuroconstruct.utils.units.*;
import ucl.physiol.neuroconstruct.utils.xml.*;

/**
 * Generates the hoc file for a specific cell template
 *
 * @author Padraig Gleeson
 *  
 */

public class NeuronTemplateGenerator
{
    static ClassLogger logger = new ClassLogger("NeuronTemplateGenerator");

    Cell cell = null;

    Project project = null;

    File hocFile = null;

    // due to limitation in hoc interpreter, when too many lines are in a procedure
    int maxNumLinesInProc = 100;
    int numSegments = 1;

    boolean addGrowthFunctions = true;
    boolean addSegIdFunctions = true;
    
    private static boolean warnedOfParamGrpSpherSegs = false;

    //Hashtable arrayNamesVsSections = new Hashtable();

    private NeuronTemplateGenerator()
    {
    }

    /**
     * Generates the HocTemplate for a single cell.
     * @param cell The Cell to generate for
     * @param dirForHocFile the Directory to getnerate into
     * @param addGrowthFunctions true if the extra functions, variables need to
     *        be added to allow growth of dends, etc.
     */
    public NeuronTemplateGenerator(Project project, 
                                    Cell cell, 
                                    File dirForHocFile, 
                                    boolean addGrowthFunctions,
                                    boolean addSegIdFunctions)
    {
        logger.logComment("HocTemplateGenerator created for: "+ CellTopologyHelper.printShortDetails(cell));
        this.cell = cell;
        this.project = project;

        StringBuffer spaceLessName = new StringBuffer();

        for (int i = 0; i < cell.getInstanceName().length(); i++)
        {
            char c = cell.getInstanceName().charAt(i);
            if ( c != ' ') spaceLessName.append(c);
        }
        hocFile = new File(dirForHocFile,  spaceLessName + ".hoc");

        this.addGrowthFunctions = addGrowthFunctions;
        this.addSegIdFunctions = addSegIdFunctions;

    }


    public String getHocFilename()
    {
        return this.hocFile.getAbsolutePath();
    }

    public String getHocShortFilename()
    {
        return this.hocFile.getName();
    }



    public String generateFile() throws NeuronException
    {
        logger.logComment("Starting generation of template file: "+hocFile);

        FileWriter fw = null;
        try
        {
            fw = new FileWriter(hocFile);

            fw.write("\n");
            fw.write(getHocFileHeader());
            if (cell.getParameterisedGroups().size()>0)
            {
                fw.write("\nload_file(\"subiter.hoc\")\n");
            }
            fw.write("\nbegintemplate "+ cell.getInstanceName()+"\n\n");
            fw.write(this.getCommonHeader());
            fw.write(this.getHeaderForGroups());
            fw.write(this.getProcInit());
            fw.write(this.createSections());


            fw.write(this.getProcTopol());
            fw.write(this.getProcBasicShape());
            fw.write(this.getProcSubsets());
            fw.write(this.getProcGeom());
            
            fw.write(this.getProcBiophys());
            fw.write(this.getProcGeomNseg());
            
            if (cell.getParameterisedGroups().size()>0)
            {
                fw.write(this.getProcBiophysInhomo());
                if (cell.getVarMechsVsParaGroups().size()>0)
                {
                    for(Segment seg: cell.getAllSegments())
                    {
                        if (!warnedOfParamGrpSpherSegs && seg.isSpherical())
                        {
                            warnedOfParamGrpSpherSegs = true;
                            GuiUtils.showWarningMessage(logger, "Warning, using parameterised groups in a cell with a spherical segment: "+seg+"\n" +
                                "This can lead to inconsistencies, as spherical segments are mapped to cylinders (length >0) in NEURON\n" +
                                "with child segments attached at the 0.5 point, and so the 'distance from the soma' etc. will include this\n" +
                                "extra part of that segment. \n\n" +
                                "Try to manually convert the spherical segment to an explicit cylinder in neuroConstruct for consistency.", null);
                        }
                    }
                }
            }
            
            fw.write(this.getProcPosition());

            if (addSegIdFunctions)
            {
                fw.write(this.getSegIdFunctions());
            }
            
            
            fw.write(this.getProcConnect2target());
            ///////////fw.write(this.getProcSynapses());
            fw.write(this.getProcInfo());

            fw.write("\nendtemplate "+ cell.getInstanceName()+"\n\n");
            fw.flush();
            fw.close();
        }
        catch (Exception ex)
        {
            logger.logError("Error writing to file: " + hocFile, ex);
            try
            {
                fw.flush();
                fw.close();
            }
            catch (IOException ex1)
            {
            }
            throw new NeuronException("Error writing to file: " + hocFile);

        }

        return hocFile.getAbsolutePath();
    }


    private static String getHocFileHeader()
    {
        StringBuffer response = new StringBuffer();
        response.append("//  ******************************************************\n");
        response.append("//\n");
        response.append("//     File generated by: neuroConstruct v"+GeneralProperties.getVersionNumber()+"\n");
        response.append("//\n");
        response.append("//     Generally replicates hoc for Cell Type as exported from\n");
        response.append("//     NEURON's Cell Builder, together with some neuroConstruct\n");
        response.append("//     specific helper/info procedures, e.g. toString(), netInfo()\n");
        response.append("//\n");
        response.append("//  ******************************************************\n");

        response.append("\n");
        return response.toString();
    }


    private String getCommonHeader()
    {
        logger.logComment("calling getCommonHeader");
        StringBuffer response = new StringBuffer();

        response.append("public init, topol, basic_shape, subsets, geom, memb\n");
        response.append("public synlist, x, y, z, position, connect2target\n\n");
        NeuronFileManager.addHocComment(response, "Some fields for referencing the cells");
        response.append("public reference, type, description, name\n");
        response.append("strdef reference, type, description, name\n\n");
        NeuronFileManager.addHocComment(response, "Some methods for referencing the cells");
        response.append("public toString, netInfo\n\n");



        if (addGrowthFunctions)
        {
            NeuronFileManager.addHocComment(response, " Needed for variable morphology\n");
            response.append("public add_dendritic_section, add_axonal_section\n");
            response.append("public additional_dends, additional_axons\n\n");
            response.append("public specify_num_extra_dends, specify_num_extra_axons\n\n");
        }
        
        if (addSegIdFunctions)
        {
            NeuronFileManager.addHocComment(response, " Needed to match segment id to NEURON sections");

            response.append("public accessSectionForSegId\n");
            response.append("public getFractAlongSection\n\n");
            
        }

        response.append("public all\n\n");
        response.append("objref synlist\n");
        response.append("objref all\n");
        response.append("objref stringFuncs\n");


        response.append("\n");
        return response.toString();
    }

    private String getProcInit()
    {
        logger.logComment("calling getProcInit");
        StringBuffer response = new StringBuffer();
        response.append("proc init() {\n");

        if (addGrowthFunctions)
        {
            if (cell.getOnlyAxonalSegments().size() > 0)
            {
                response.append("create_dummy_axons()\n");
            }

            if (cell.getOnlyDendriticSegments().size() > 0)
            {
                response.append("create_dummy_dends()\n");
            }
        }

        response.append("    topol()\n");
        response.append("    subsets()\n");
        response.append("    geom()\n");
        response.append("    biophys()\n");
        response.append("    geom_nseg()\n");
        
        if (cell.getParameterisedGroups().size()>0)
        {
            response.append("    biophys_inhomo()\n");
        }
        
        response.append("    synlist = new List()\n");
        /////////////response.append("    synapses()\n");
        response.append("    x = y = z = 0\n");


        //response.append("name = $s1\n");
        response.append("    reference = $s1\n");
        response.append("    type = $s2\n");
        response.append("    description = $s3\n");
        response.append("    \n");
        response.append("    strdef indexNum\n");
        response.append("    stringFuncs = new StringFunctions()\n");

        response.append("    stringFuncs.tail(reference, \"_\", indexNum)\n");

        response.append("    while (stringFuncs.substr( indexNum, \"_\")>=0) {\n");

        response.append("        stringFuncs.tail(indexNum, \"_\", indexNum)\n");

        response.append("    }\n");


        response.append("    \n");


        //response.append("    sprint(name, \"%s[%s]\", type, indexNum)\n");
        response.append("    sprint(name, \"%s\", type)\n");




        response.append("}\n");
        response.append("\n");
        return response.toString();
    }

    private String getProcInfo()
    {
        logger.logComment("calling getProcInfo");
        StringBuffer response = new StringBuffer();

        NeuronFileManager.addHocComment(response, "This function is useful when checking what cells (aot sections) have been created. Run allcells() from nCtools.hoc...");
        response.append("proc toString() {\n");
        response.append("    strdef info\n");

        response.append("    sprint(info, \"Cell ref: %s (%s), at: (%d, %d, %d)\", reference, name, x, y, z)\n");

        response.append("    print info\n");


        response.append("}\n");
        response.append("\n");


        NeuronFileManager.addHocComment(response, "This function is useful when checking network connections");
        response.append("proc netInfo() {\n");


        response.append("    strdef info\n");

        response.append("    sprint(info, \"Cell reference: %s, type: %s\", reference, type)\n");

        response.append("    print \"--------  \",info\n");



        response.append("    print \"    There are \", synlist.count(), \" connections in \", synlist\n");

        response.append("    for i=0,synlist.count()-1 {\n");
        response.append("        print \"        Connection from \", synlist.o[i].precell, \" to: \", synlist.o[i].postcell\n");
        response.append("        print \"        Pre:   Weight: \", synlist.o[i].weight, \", delay: \", synlist.o[i].delay, \", threshold: \", synlist.o[i].threshold \n");
        response.append("        print \"        Post:  \", synlist.o[i].syn(), \", gmax: \", synlist.o[i].syn().gmax , \", e: \", synlist.o[i].syn().e , \", rise time: \", synlist.o[i].syn().tau_rise , \", decay time: \", synlist.o[i].syn().tau_decay \n");

        response.append("    print \" \"\n");
        response.append("    }\n");
        response.append("    \n");
        response.append("    \n");
        response.append("    \n");


        response.append("    print \"--------  \"\n");
        response.append("    print \" \"\n");


        response.append("}\n");
        response.append("\n");


        return response.toString();
    }






    private String createSections()
    {
        logger.logComment("creating Sections");
        StringBuffer response = new StringBuffer();

        ArrayList<Section> sections = cell.getAllSections();
        if (sections.size() > 0)
        {
            Hashtable<String, Integer> arraySectionsVsSize = new Hashtable<String, Integer>();

            for (int i = 0; i < sections.size(); i++)
            {
                Section next = sections.get(i);

                if (cell.getApPropSpeedForSection(next)==null)
                {
                    String name = NeuronFileManager.getHocSectionName(next.getSectionName());
                    /////System.out.println(name);

                    if (name.indexOf("[") > 0)
                    {
                        String arrayName = name.substring(0, name.indexOf("["));
                        Integer greatestIndexSoFar = arraySectionsVsSize.get(arrayName);
                        int thisIndex
                            = Integer.parseInt(name.substring(name.indexOf("[") + 1,
                                                              name.indexOf("]")));

                        if (greatestIndexSoFar == null
                            || greatestIndexSoFar.intValue() < thisIndex)
                        {
                            arraySectionsVsSize.put(arrayName, thisIndex);
                        }
                    }
                    else
                    {
                        response.append("create " + name + "\n");
                        response.append("public " + name + "\n");
                    }
                }
            }
            Enumeration arrays = arraySectionsVsSize.keys();

            while (arrays.hasMoreElements())
            {
                String arr = (String)arrays.nextElement();
                logger.logComment("Looking at array: "+arr);
                Integer maxIndex = arraySectionsVsSize.get(arr);
                response.append("create " + arr +"["+(maxIndex.intValue()+1)+"]"+ "\n");
                response.append("public " + arr + "\n");

            }
        }
        response.append("\n");


        return response.toString();
    }




    private String getProcTopol()
    {
        logger.logComment("calling getProcTopol");
        StringBuffer response = new StringBuffer();
        response.append("proc topol() {\n");

        Vector allConnectionLines =  getConnectLines();

        if (allConnectionLines.size()<maxNumLinesInProc)
        {
            logger.logComment("Only have "+allConnectionLines.size()+" connections, putting them in one function");

            for (int i = 0; i < allConnectionLines.size(); i++)
            {
                String nextLine = (String)allConnectionLines.elementAt(i);
                response.append(nextLine+"\n");
            }
            response.append("    basic_shape()\n");
            response.append("}\n");
            response.append("\n");

        }
        else
        {
            logger.logComment("Will have to split up the "+allConnectionLines.size()+" connections...");
            int numberToSplit = Math.round((float)allConnectionLines.size()/(float)maxNumLinesInProc) + 1;
            logger.logComment("Will create "+numberToSplit+" functions...");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("    topol_extra_"+i+"()\n");
            }
            response.append("    basic_shape()\n");
            response.append("}\n");
            response.append("\n");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("proc topol_extra_" + i + "(){\n");
                for (int j = 0; j < maxNumLinesInProc; j++)
                {
                    try
                    {
                        int index = (i*maxNumLinesInProc) + j;
                        String nextLine = (String) allConnectionLines.elementAt(index);
                        response.append(nextLine + "\n");
                    }
                    catch (Exception ex)
                    {
                        // no element, i.e. past the end of the array...
                    }
                }
                response.append("}\n");
                response.append("\n");
            }
        }



        return response.toString();
    }


    private Vector<String> getConnectLines()
    {
        Vector<String> connectLines = new Vector<String>();

        Vector segments = cell.getExplicitlyModelledSegments();

        logger.logComment("Investigating "+segments.size()+" segments...");


        for (int i = 0; i < segments.size(); i++)
        {
            Segment segment = (Segment)segments.elementAt(i);

            logger.logComment("Looking at segment number "+i+": "+segment);

            Segment parent = segment.getParentSegment();

            logger.logComment("Parent of this is: "+parent);

            if (parent!=null &&
                segment.isFirstSectionSegment() &&
                !segment.getSection().equals(parent.getSection()))
            {
                //float distToConnectTo

                float fractionAlongParentSection =
                    CellTopologyHelper.getFractionAlongSection(cell,
                                                               segment.getParentSegment(),
                                                               segment.getFractionAlongParent());

                if (fractionAlongParentSection>0.999f)
                    fractionAlongParentSection = 1; // to remove rounding errors
                
                connectLines.add("    connect " + NeuronFileManager.getHocSectionName(segment.getSection().getSectionName())
                                 + "(0), "
                                 + NeuronFileManager.getHocSectionName(parent.getSection().getSectionName())
                                 + "("
                                 + fractionAlongParentSection
                                 + ")");
            }

        }
        return connectLines;
    }




    private String getProcBasicShape()
    {
        logger.logComment("--------------------------------calling getProcBasicShape");
        StringBuffer response = new StringBuffer();
        response.append("proc basic_shape() {\n");

        Vector allShapeLines = getShapeLines();


        if (allShapeLines.size() < maxNumLinesInProc)
        {
            logger.logComment("Only have " + allShapeLines.size() +" lines of shape info, putting them in one function");
            for (int i = 0; i < allShapeLines.size(); i++)
            {
                String nextLine = (String) allShapeLines.elementAt(i);
                response.append(nextLine + "\n");
            }
            response.append("}\n");
            response.append("\n");
        }
        else
        {
            logger.logComment("Will have to split up the " + allShapeLines.size() +" shape info lines...");
            int numberToSplit = Math.round( (float) allShapeLines.size() / (float) maxNumLinesInProc) +  1;
            logger.logComment("Will create " + numberToSplit + " functions...");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("basic_shape_extra_" + i + "()\n");
            }
            response.append("}\n");
            response.append("\n");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("proc basic_shape_extra_" + i + "(){\n");
                for (int j = 0; j < maxNumLinesInProc; j++)
                {
                    try
                    {
                        int index = (i * maxNumLinesInProc) + j;
                        String nextLine = (String) allShapeLines.elementAt(index);
                        response.append(nextLine + "\n");
                    }
                    catch (Exception ex)
                    {
                        // no element, i.e. past the end of the array...
                    }
                }
                response.append("}\n");
                response.append("\n");
            }
        }
        return response.toString();
    }


    /**
     * Returns a start point for the cylinder (i.e. section start point) which will
     * represent the spherical segment
     *
     */
    public static Point3f getSphericalSegmentStartPoint(Segment segment, Cell cell)
    {
        if (segment.getSegmentShape() != Segment.SPHERICAL_SHAPE)
        {
            logger.logError("Segment "+ segment+ " is not spherical");
            return null;
        }
        if (!cell.getAllSegments().contains(segment))
        {
            logger.logError("Segment "+ segment+ " is not part of cell: "+ cell);
            return null;
        }
        //Suggest a point along the z axis, the distance of the radius...

        Point3f proposedPoint = new Point3f(0,-1*segment.getRadius(),0);

        // For purely aesthetic purposes
       // Vector allSegs = cell.getAllSegments();
      //  for (int i = 0; i < allSegs.size(); i++)
      //  {
      //          Segment nextSeg = (Segment)allSegs.elementAt(i);
      //          if (nextSeg.getParentSegment()==)

     //   }
         return proposedPoint;

    }


    /**
     * Returns the end point for the cylinder (i.e. section end point) which will
     * represent the spherical segment
     *
     */
    public static Point3f getSphericalSegmentEndPoint(Segment segment, Cell cell)
    {
        if (segment.getSegmentShape() != Segment.SPHERICAL_SHAPE)
        {
            logger.logError("Segment "+ segment+ " is not spherical");
            return null;
        }
        if (!cell.getAllSegments().contains(segment))
        {
            logger.logError("Segment "+ segment+ " is part of cell: "+ cell);
            return null;
        }
        //Suggest a point along the z axis, the distance of the radius...

        Point3f proposedPoint = new Point3f(0,segment.getRadius(),0);

        // For purely aesthetic purposes
       // Vector allSegs = cell.getAllSegments();
      //  for (int i = 0; i < allSegs.size(); i++)
      //  {
      //          Segment nextSeg = (Segment)allSegs.elementAt(i);
      //          if (nextSeg.getParentSegment()==)

     //   }
         return proposedPoint;

    }



    private Vector getShapeLines()
    {
        Vector segments = cell.getExplicitlyModelledSegments();

        logger.logComment("Investigating " + segments.size() + " segments...");
        Vector<String> shapeLines = new Vector<String>();

        for (int i = 0; i < segments.size(); i++)
        {
            Segment segment = (Segment) segments.elementAt(i);
            Segment parent = segment.getParentSegment();

            if (NeuronFileManager.addComments()) shapeLines.add("\n//  Looking at segment number " + i + ": "+ segment);

            if (segment.isSomaSegment() && segment.isFirstSectionSegment())
            {
                if (segment.getSegmentShape() == Segment.CYLINDRICAL_SHAPE)
                {
                    Point3f startPoint = segment.getSection().getStartPointPosition();
                    Point3f endPoint = segment.getEndPointPosition();

                    shapeLines.add("    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName()) +" {pt3dclear() pt3dadd("
                                    + startPoint.x + ", " + startPoint.y + ", " + startPoint.z + ", "
                                    + (segment.getSection().getStartRadius() * 2)
                                    + ") pt3dadd("
                                    + endPoint.x + ", " + endPoint.y + ", " + endPoint.z + ", "
                                    + (segment.getRadius() * 2) + ")}");
                }
                else if (segment.getSegmentShape() == Segment.SPHERICAL_SHAPE)
                {
                    Point3f centrePoint = segment.getEndPointPosition();
                    
                    Point3f startPoint = getSphericalSegmentStartPoint(segment, cell);
                    startPoint.add(centrePoint);
                    
                    Point3f endPoint = getSphericalSegmentEndPoint(segment, cell);
                    endPoint.add(centrePoint);

                    shapeLines.add("    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName())
                                   + " {pt3dclear() pt3dadd("
                                   + startPoint.x + ", " + startPoint.y + ", " + startPoint.z + ", "
                                   + (segment.getRadius() * 2)
                                   + ") pt3dadd("
                                   + endPoint.x + ", " + endPoint.y + ", " + endPoint.z + ", "
                                   + (segment.getRadius() * 2) + ")}");
                }
            }
            else
            {
              /*  if (parent.isSomaSegment()  &&
                    parent.isFirstSectionSegment() &&
                    parent.getSegmentShape() == Segment.SPHERICAL_SHAPE &&
                    !segment.isSomaSegment())
                {
                    shapeLines.add("hh    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName()) 
                                   + " {pt3dclear() pt3dadd(0,0,0,"
                                   + (segment.getRadius() * 2) + ") "
                                   + "pt3dadd("
                                   + segment.getEndPointPositionX() + ","
                                   + segment.getEndPointPositionY() + ","
                                   + segment.getEndPointPositionZ() + ", "
                                   + (segment.getRadius() * 2) + ")}");
                }
                else
                {*/
                Point3f startPoint = segment.getStartPointPosition();
                float startRadius = segment.getSegmentStartRadius();
/*
                if (segment.isFirstSectionSegment())
                {
                    startPoint = segment.getSection().getStartPointPosition();
                    startRadius = segment.getSection().getStartRadius();
                }
                else
                {
                    startPoint = segment.getParentSegment().getEndPointPosition();
                    startRadius = segment.getParentSegment().getRadius();
                }*/

                StringBuffer lineToAdd = new StringBuffer("    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName()) + " {");

                if (segment.isFirstSectionSegment())
                {
                    lineToAdd.append("pt3dclear() ");

                    lineToAdd.append("pt3dadd(" + startPoint.x + ", "
                                     + startPoint.y + ", "
                                     + startPoint.z + ", "
                                     + (startRadius * 2) + ") ");
                }

                lineToAdd.append("pt3dadd("
                               + segment.getEndPointPositionX() + ", "
                               + segment.getEndPointPositionY() + ", "
                               + segment.getEndPointPositionZ() + ", "
                               + (segment.getRadius() * 2) + ")}");

                 
                shapeLines.add(lineToAdd.toString());
               // }
            }

        }
        return shapeLines;
    }



    private String getProcSubsets()
    {
        logger.logComment("---------  calling getProcSubsets");

        StringBuffer response = new StringBuffer();

        Vector<String> subsetLines = new Vector<String>();

        logger.logComment("Adding channel mechs for soma (etc)");

        Vector groupNames = cell.getAllGroupNames();

        for (int ii = 0; ii < groupNames.size(); ii++)
        {
            String groupName = (String) groupNames.elementAt(ii);
            if (!groupName.equals("all"))
            {
                logger.logComment("Found a group: " + groupName);

                subsetLines.add("    "+groupName + " = new SectionList()\n");

                /** @todo This could be cleaned up... */
                Vector allSegments = cell.getExplicitlyModelledSegments();

                for (int i = 0; i < allSegments.size(); i++)
                {
                    Segment segment = (Segment) allSegments.elementAt(i);
                    if (segment.isFirstSectionSegment())
                    {
                        if (segment.getGroups().contains(groupName))
                        {
                            subsetLines.add("    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName()) + " " + groupName + ".append()");
                        }
                    }
                }
            }

            subsetLines.add("\n");

        }

        Vector segments = cell.getExplicitlyModelledSegments();

        for (int i = 0; i < segments.size(); i++)
        {
            Segment segment = (Segment) segments.elementAt(i);
            if (segment.isFirstSectionSegment())
            {
                subsetLines.add("    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName()) + " all.append()");
            }

        }


        response.append("proc subsets() { local i\n\n");

        NeuronFileManager.addHocComment(response, "The group all is assumed never to change", false);

        response.append("    "+"all = new SectionList()\n");


        if (subsetLines.size() < maxNumLinesInProc)
        {
            logger.logComment("..............     Only have " + subsetLines.size() +
                              " lines of subset info, putting them in one function");
            for (int i = 0; i < subsetLines.size(); i++)
            {
                String nextLine = subsetLines.elementAt(i);
                response.append(nextLine+"\n");
            }
            response.append("}\n");
            response.append("\n");
        }
        else
        {
            logger.logComment("Will have to split up the " + subsetLines.size() + " subset info lines...");
            int numberToSplit = Math.round( (float) subsetLines.size() / (float) maxNumLinesInProc) + 1;
            logger.logComment("Will create " + numberToSplit + " functions...");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("subsets_extra_" + i + "()\n");
            }
            response.append("}\n");
            response.append("\n");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("proc subsets_extra_" + i + "(){\n");
                for (int j = 0; j < maxNumLinesInProc; j++)
                {
                    try
                    {
                        int index = (i * maxNumLinesInProc) + j;
                        String nextLine = subsetLines.elementAt(index);
                        response.append(nextLine + "\n");
                    }
                    catch (Exception ex)
                    {
                        // no element, i.e. past the end of the array...
                    }
                }
                response.append("}\n");
                response.append("\n");
            }
            }

        logger.logComment("---------  finished getProcSubsets");

        return response.toString();
    }


    private String getHeaderForGroups()
    {
        logger.logComment("calling getHeaderForGroups");

        StringBuffer response = new StringBuffer();

        Vector groupNames = cell.getAllGroupNames();
        for (int ii = 0; ii < groupNames.size(); ii++)
        {
            String groupName = (String)groupNames.elementAt(ii);
            if (!groupName.equals("all"))
            {
                response.append("public " + groupName + "\n");
                response.append("objref " + groupName + "\n");
            }
        }
        response.append("\n");
        return response.toString();
    }



    private String getProcGeom()
    {
        logger.logComment("calling getProcGeom");
        StringBuffer response = new StringBuffer();
        response.append("proc geom() {\n");
/*
        Soma soma = cell.getSomaDetails();
        response.append("soma {  L = "+soma.getLength()+"  diam = "+soma.getDiameter()+"  }\n");

        Axon axon = cell.getAxonDetails();
        response.append("axon {  L = "+axon.getLength()+"  diam = "+axon.getDiameter()+"  }\n");

        Dendrite[] dendrites = cell.getDendrites();
        for (int i = 0; i < dendrites.length; i++)
        {
            response.append("dend["+i+"] {  L = "+dendrites[i].getLength()+" diam = "+dendrites[i].getDiameter()+"  }\n");
        }

            */
        response.append("}\n");
        response.append("\n");
        return response.toString();
    }






    private String getProcGeomNseg()
    {
        logger.logComment("calling getProcGeomNseg");
        StringBuffer response = new StringBuffer();
        //response.append("//external lambda_f\n");


        Vector<String> nsegLines = new Vector<String>();

        Vector allSegments = cell.getExplicitlyModelledSegments();
        
        if (NeuronFileManager.addComments())
            nsegLines.add("    // All sections not mentioned here have nseg = 1\n");
        
        for (int i = 0; i < allSegments.size(); i++)
        {
            Segment segment = (Segment) allSegments.elementAt(i);
            if (segment.isFirstSectionSegment())
            {
                if (segment.getSection().getNumberInternalDivisions()!=1)
                {
                    nsegLines.add("    "+NeuronFileManager.getHocSectionName(segment.getSection().getSectionName())
                                    + " nseg = "
                                    + segment.getSection().getNumberInternalDivisions());
                }

            }
        }
        response.append("proc geom_nseg() {\n");

        if (nsegLines.size() < maxNumLinesInProc)
        {
            logger.logComment("..............     Only have " + nsegLines.size() +
                              " nsegLines, putting them in one function");

            for (int i = 0; i < nsegLines.size(); i++)
            {
                String nextLine = nsegLines.elementAt(i);
                response.append(nextLine + "\n");
            }
            response.append("}\n");
            response.append("\n");
        }
        else
        {
            logger.logComment("Will have to split up the " + nsegLines.size() + " nsegLines...");

            int numberToSplit = Math.round( (float) nsegLines.size() / (float) maxNumLinesInProc) + 1;
            logger.logComment("Will create " + numberToSplit + " functions...");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("geom_nseg_extra_" + i + "()\n");
            }
            response.append("}\n");
            response.append("\n");

            for (int i = 0; i < numberToSplit; i++)
            {
                response.append("proc geom_nseg_extra_" + i + "(){\n");
                for (int j = 0; j < maxNumLinesInProc; j++)
                {
                    try
                    {
                        int index = (i * maxNumLinesInProc) + j;
                        String nextLine = nsegLines.elementAt(index);
                        response.append(nextLine + "\n");
                    }
                    catch (Exception ex)
                    {
                        // no element, i.e. past the end of the array...
                    }
                }
                response.append("}\n");
                response.append("\n");
            }
            }

        response.append("\n");
        return response.toString();
    }


    private String getProcBiophys()
    {
        logger.logComment("calling getProcBiophys");
        StringBuffer response = new StringBuffer();
        
        StringBuffer pointProcessCreates = new StringBuffer();
                        
        response.append("proc biophys() {\n");
        Vector<String> groupNames = cell.getAllGroupNames();
        
        Hashtable<String, Boolean> revPotSetElsewhereHash = new Hashtable<String, Boolean>();

        for (String nextGroup: groupNames)
        {
            float specCap = cell.getSpecCapForGroup(nextGroup);
            if (!Float.isNaN(specCap))
            {
                response.append("    forsec " + nextGroup + " cm = "
                                + UnitConverter.getSpecificCapacitance(specCap,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS,
                                                                       UnitConverter.NEURON_UNITS)+ "\n");
            }
            float specAxRes = cell.getSpecAxResForGroup(nextGroup);
            if (!Float.isNaN(specAxRes))
            {
                response.append("    forsec " + nextGroup + " Ra = " +
                                UnitConverter.getSpecificAxialResistance(specAxRes,
                                                                         UnitConverter.NEUROCONSTRUCT_UNITS,
                                                                         UnitConverter.NEURON_UNITS)+ "\n");
            }
        }
        response.append("\n");


        if (cell.getChanMechsVsGroups().size()>100)
        {
            response.append("    addChanMechs()\n");
            response.append("}\n\n");
            response.append("proc addChanMechs() {\n\n");
        }

        int totLines = 0;
        int subProcCount = 0;

        for (String nextGroup: groupNames)
        {
            logger.logComment("nextGroup: "+nextGroup+"--------------------------------");
            ArrayList<ChannelMechanism> allChanMechs = cell.getChanMechsForGroup(nextGroup);
            
            
            if (allChanMechs.size()>0)
            {
                for (int j = 0; j < allChanMechs.size(); j++)
                {

                    StringBuffer subResponse = new StringBuffer();

                    ChannelMechanism nextChanMech = allChanMechs.get(j);

                    CellMechanism cellMech = project.cellMechanismInfo.getCellMechanism(nextChanMech.getName());
                    
                    if (cellMech.isPointProcess())
                    {
                        ArrayList<Section> secs = cell.getSectionsInGroup(nextGroup);
                        for(Section sec:secs)
                        {
                            String name = "pp_"+cellMech.getInstanceName()+"_"+sec.getSectionName();
                            pointProcessCreates.append("public "+name+"\n");
                            pointProcessCreates.append("objref "+name+"\n\n");
                            
                            subResponse.append("    "+sec.getSectionName()+" "+name+" = new " + cellMech.getInstanceName() + "(0.5) \n");
                        }
                    }
                    else
                    {
                        boolean requiresXYZ = false;

                        try
                        {
                            requiresXYZ = cellMech instanceof AbstractedCellMechanism &&
                                ( (AbstractedCellMechanism) cellMech).parameterExists("RequiresXYZ") &&
                                ( (AbstractedCellMechanism) cellMech).getParameter("RequiresXYZ") == 1;
                        }
                        catch (CellMechanismException ex1)
                        {
                            logger.logComment("No param RequiresXYZ...");
                        }


                        subResponse.append("    forsec " + nextGroup + " { ");

                        ArrayList<MechParameter> mps = nextChanMech.getExtraParameters();
                        
                        if (mps.size()==0)
                        {
                            NeuronFileManager.addHocComment(subResponse,
                                                     "    Assuming parameters other than max cond dens are set in the mod file...");
                        }
                        else
                        {
                            NeuronFileManager.addHocComment(subResponse,
                                                     "    Using parameters: "+mps);
                        }
                        
                        subResponse.append("        insert " + nextChanMech.getName() + "");

                        double condDens = UnitConverter.getConductanceDensity(nextChanMech.getDensity(),
                                                                              UnitConverter.NEUROCONSTRUCT_UNITS,
                                                                              UnitConverter.NEURON_UNITS);
                                                                              
                        StringBuffer moreParams = new StringBuffer();
                        
                        for (MechParameter mp: mps)
                        {
                            if(!mp.getName().equals("erev")) // will be checked for later...
                            {
                                moreParams.append("\n    "+mp.getName()+"_"+nextChanMech.getName()+" = "+ mp.getValue());
                            }
                        }


                        if (cellMech.getMechanismType().equals(CellMechanism.CHANNEL_MECHANISM))
                        {
                            if (nextChanMech.getName().equals("pas"))
                            {
                                String condString = "g_" + nextChanMech.getName() + " = " + condDens + moreParams.toString();
                                if (condDens<0) condString = "    // Ignoring gmax ("+ condDens+") for this channel mechanism\n";
                                
                                subResponse.append("  { "+condString+ " }  ");
                                NeuronFileManager.addHocComment(subResponse,
                                                     "    pas is name of mechanism, so using inbuilt mechanism, and better use g for conductance density...");
                            }
                            else
                            {
                                String condString = "gmax_" + nextChanMech.getName() + " = " + condDens;
                                if (condDens<0) condString = "\n    // Ignoring gmax ("+ condDens+") for this channel mechanism\n";
                                
                                subResponse.append("  { "+condString + moreParams.toString() +" }  ");
                            }
                        }
                        else if (cellMech.getMechanismType().equals(CellMechanism.ION_CONCENTRATION))
                        {
                            
                                subResponse.append("  { "+moreParams.toString() +" }  ");
                            
                        }

                        if (cellMech instanceof ChannelMLCellMechanism)
                        {
                            ChannelMLCellMechanism cmlMech = (ChannelMLCellMechanism) cellMech;

                            String xpath = ChannelMLConstants.getPreV1_7_3IonsXPath();

                            logger.logComment("Checking xpath: " + xpath);

                            SimpleXMLEntity[] ions = null;
                            try
                            {
                                cmlMech.initialise(project, false); // just in case...

                                ions = cmlMech.getXMLDoc().getXMLEntities(xpath);
                            }
                            catch (ChannelMLException ex)
                            {
                                GuiUtils.showErrorMessage(logger,
                                                          "Error getting information from ChannelML process: "
                                                          + cmlMech.getInstanceName(), ex, null);

                                return "Error in generation: " + ex.getMessage();
                            }
                            boolean postV1_7_3 = false;
                            
                            if (ions == null || ions.length == 0)
                            {
                                // Test for v1.7.3 preferered form..
                                
                                xpath = ChannelMLConstants.getIonNameXPath();
                                try
                                {
                                    String ionName = cmlMech.getXMLDoc().getValueByXPath(xpath);
                                    logger.logComment("--- Got ion: " + ionName+ " for cell mech: "+cellMech);
                                    if (ionName!=null)
                                    {
                                        postV1_7_3 = true;
                                        
                                        xpath = ChannelMLConstants.getCurrVoltRelXPath();
                                        ions = cmlMech.getXMLDoc().getXMLEntities(xpath);
                                    }
                                }
                                catch (ChannelMLException ex)
                                {
                                    return "Error in getting ion name: " + ex.getMessage();
                                }
                                
                            }

                            if (ions != null && ions.length > 0)
                            {
                                for (int i = 0; i < ions.length; i++)
                                {
                                    SimpleXMLElement ionEl = (SimpleXMLElement) ions[i];
                                    String ionName = null;
                                    String erev = ionEl.getAttributeValue(ChannelMLConstants.ION_REVERSAL_POTENTIAL_ATTR);
                                    
                                    if (!postV1_7_3)
                                        ionName = ionEl.getAttributeValue(ChannelMLConstants.LEGACY_ION_NAME_ATTR);
                                    else
                                        ionName = ionEl.getAttributeValue(ChannelMLConstants.NEW_ION_NAME_ATTR);
                                    
                                        
                                    logger.logComment("Got ion: " + ionName+ ", rev pot: "+erev+" for cell mech: "+cellMech);

                                    NeuronFileManager.addHocComment(subResponse, "    Ion " + ionName + " is used in this process...");

                                    String unitsUsed = cmlMech.getUnitsUsedInFile();

                                    
                                    MechParameter mpErev = nextChanMech.getExtraParameter("e");
                                    if (mpErev==null)
                                        mpErev = nextChanMech.getExtraParameter("erev");
                                    
                                    if (mpErev!=null)
                                    {
                                        
                                            if (unitsUsed.equals(ChannelMLConstants.SI_UNITS))
                                            {
                                                erev = ""+UnitConverter.getVoltage( mpErev.getValue(),
                                                                                         UnitConverter.GENESIS_SI_UNITS,
                                                                                         UnitConverter.NEUROCONSTRUCT_UNITS);
                                            }
                                            else if (unitsUsed.equals(ChannelMLConstants.PHYSIOLOGICAL_UNITS))
                                            {
                                                erev = ""+UnitConverter.getVoltage( mpErev.getValue(),
                                                                                         UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS,
                                                                                         UnitConverter.NEUROCONSTRUCT_UNITS);
                                            }
                                    }

                                    logger.logComment("Setting erev: " + erev+" for "+nextChanMech);
                                    
                                    
                                    if (erev != null)
                                    {
                                        logger.logComment("Units used = " + unitsUsed);

                                        if (unitsUsed != null)
                                        {
                                            double suggValDouble = Double.parseDouble(erev);

                                            if (unitsUsed.equals(ChannelMLConstants.SI_UNITS))
                                            {
                                                suggValDouble = UnitConverter.getVoltage(suggValDouble,
                                                                                         UnitConverter.GENESIS_SI_UNITS,
                                                                                         UnitConverter.NEUROCONSTRUCT_UNITS);
                                            }
                                            else if (unitsUsed.equals(ChannelMLConstants.PHYSIOLOGICAL_UNITS))
                                            {
                                                suggValDouble = UnitConverter.getVoltage(suggValDouble,
                                                                                         UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS,
                                                                                         UnitConverter.NEUROCONSTRUCT_UNITS);
                                            }

                                            erev = suggValDouble + "";
                                        }
                                        
                                        //if (!revPotSetElsewhereHash.containsKey(nextChanMech.getName()))
                                        //{
                                            logger.logComment("Recheking revPotSetElsewhere for "+ nextChanMech.getName());
                                            boolean revPotSetElsewhere = false;
                                            
                                            Hashtable<ChannelMechanism, Vector<String>> cmVsGroups = cell.getChanMechsVsGroups();

                                            Enumeration<ChannelMechanism> cms = cmVsGroups.keys();

                                            while (cms.hasMoreElements())
                                            {
                                                ChannelMechanism cm = cms.nextElement();
                                                if (cm.getName().equals(nextChanMech.getName()))
                                                {
                                                    Vector<String> groups = cmVsGroups.get(cm);
                                                    
                                                    for(String grp: groups)
                                                    {
                                                        if (!grp.equals(nextGroup) /*|| nextChanMech.getName().equals("pas")*/)
                                                        {
                                                            NeuronFileManager.addHocComment(subResponse, "    Group " + grp +" also has "+ nextChanMech.getName()+" ("+cm+")", false);

                                                            if (CellTopologyHelper.isGroupASubset(nextGroup, grp, cell))
                                                            {
                                                                MechParameter mpe1 = cm.getExtraParameter("e");
                                                                MechParameter mpe2 = cm.getExtraParameter("erev");

                                                                if (mpe1!=null || mpe2!=null)
                                                                {
                                                                    revPotSetElsewhere = true;

                                                                    NeuronFileManager.addHocComment(subResponse, "    Reverse potential for ion set by "
                                                                        + cm.toString()+" which is on superset group: "+ grp);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            revPotSetElsewhereHash.put(nextChanMech.getName(), revPotSetElsewhere);
                                        //}
                                        //else
                                        //{
                                        //    logger.logComment("Reusing revPotSetElsewhere for "+ nextChanMech.getName());
                                        //}
                                        
                                        revPotSetElsewhere = revPotSetElsewhereHash.get(nextChanMech.getName());
                                        
                                        logger.logComment("revPotSetElsewhere for "+ revPotSetElsewhere);
                                        
                                        if (!(ionName.equals(ChannelMLConstants.NON_SPECIFIC_ION_NAME)))
                                        {
                                            if (!revPotSetElsewhere)
                                            {
                                                subResponse.append("        e" + ionName + " = " + erev + "\n");
                                            }
                                        }
                                        else
                                        {
                                            logger.logComment("That's a non specific process...");
                                            
                                            if (!revPotSetElsewhere && nextChanMech.getName().equals("pas"))
                                            {
                                                subResponse.append("  e_" + nextChanMech.getName() + " = " + erev + "\n");
                                            }
                                        }

                                    }
                                    else
                                    {
                                        NeuronFileManager.addHocComment(subResponse,
                                                                        "Note: there is no reversal potential present for ion: " + ionName);
                                    }
                                }
                            }
                            else
                            {
                                logger.logComment("No ion present for cell mech: "+ cellMech);
                            }

                        }

                        subResponse.append("\n    }\n\n");


                        if (requiresXYZ)
                        {
                            NeuronFileManager.addHocComment(subResponse, "Need to add x,y,z coords for this mechanism...");

                            ArrayList<Section> secs = cell.getSectionsInGroup(nextGroup);
                            for (Section sec: secs)
                            {
                                Point3f midpoint = CellTopologyHelper.convertSectionDisplacement(cell, sec, 0.5f);
                                subResponse.append("    "+NeuronFileManager.getHocSectionName(sec.getSectionName())+".x_"+nextChanMech.getName()+" = "+midpoint.x+"\n");
                                subResponse.append("    "+NeuronFileManager.getHocSectionName(sec.getSectionName())+".y_"+nextChanMech.getName()+" = "+midpoint.y+"\n");
                                subResponse.append("    "+NeuronFileManager.getHocSectionName(sec.getSectionName())+".z_"+nextChanMech.getName()+" = "+midpoint.z+"\n\n");
                            }
                        }
                    }
                    int numLinesHere = 0;
                    for(int i=0;i<subResponse.length();i++)
                    {
                        if (subResponse.charAt(i)=='\n')
                            numLinesHere++;
                    }
                    if (totLines+numLinesHere>=maxNumLinesInProc)
                    {

                        response.append("    addChanMechs_"+subProcCount+"()  // Spliting function to prevent errors when proc too big\n");
                        response.append("}\n\n");
                        response.append("proc addChanMechs_"+subProcCount+"() {\n\n");
                        subProcCount++;
                        totLines = 0;

                    }
                    totLines +=numLinesHere;

                    response.append(subResponse.toString());
                }
            }

        }
        
        
        Enumeration<VariableMechanism> varMechs = cell.getVarMechsVsParaGroups().keys();
        
        while (varMechs.hasMoreElements())
        {
            VariableMechanism vm = varMechs.nextElement();
            ParameterisedGroup pg = cell.getVarMechsVsParaGroups().get(vm);
            
            response.append("    forsec " + pg.getGroup() + " { \n");
            
            NeuronFileManager.addHocComment(response, "    Variable mechanism: "+ vm,false);
            NeuronFileManager.addHocComment(response, "    On parameter group: "+ pg,false);
            NeuronFileManager.addHocComment(response, "    Note, gmax, etc. will be set in biophys_inhomo()",false);
            response.append("        insert "+vm.getName()+" { "+vm.getParam().getName()+"_"+vm.getName()+" = 0 }\n");
            
            response.append("    }\n");
        }
        

        response.append("}\n");
        response.append("\n");
        
        if (pointProcessCreates.length()>0)
        {
            return pointProcessCreates.toString()+"\n\n" + response.toString();
        }
        
        return response.toString();
    }
    
    

    private String getProcBiophysInhomo()
    {
        logger.logComment("calling getProcBiophys");
        StringBuffer response = new StringBuffer();
        for(ParameterisedGroup pg: cell.getParameterisedGroups())
        {
            response.append("objref "+pg.getName()+" \n");
        }
        StringBuffer postProcs = new StringBuffer();
        
        float convFactor = (float)UnitConverter.convertFromNeuroConstruct(1, UnitConverter.currentDensityUnits[UnitConverter.NEUROCONSTRUCT_UNITS], UnitConverter.NEURON_UNITS).getMagnitude();
         
        response.append("proc biophys_inhomo() { \n");
        
        for(ParameterisedGroup pg: cell.getParameterisedGroups())
        {
            response.append("    "+pg.getName()+" = new "+pg.getNeuronObject()+" \n");
        }
            response.append("     \n");
                
        Enumeration<VariableMechanism> varMechs = cell.getVarMechsVsParaGroups().keys();
        
        while (varMechs.hasMoreElements())
        {
            VariableMechanism vm = varMechs.nextElement();
            ParameterisedGroup pg = cell.getVarMechsVsParaGroups().get(vm);
            String procName=vm.getParam().getName()+"_"+vm.getName()+"_"+pg.getGroup()+"()";
            response.append("    "+procName+"\n");
            
            postProcs.append("proc "+procName+" { local x, p, p0, p1"+"\n");
            postProcs.append("    "+pg.getName()+".update()\n");
            postProcs.append("    p0 = "+pg.getName()+".p0  p1 = "+pg.getName()+".p1\n");
            postProcs.append("    for "+pg.getName()+".loop() {\n");
            postProcs.append("        x = "+pg.getName()+".x  p = "+pg.getName()+".p\n");
            postProcs.append("        "+vm.getParam().getName()+"_"+vm.getName()+"(x) = "+convFactor+" * "+vm.getParam().getExpression()+" // "+convFactor+" to convert from nc to NEURON units\n");
            postProcs.append("    }\n");
  
            postProcs.append("}\n\n");
        }
        
        response.append("}\n");
        response.append("\n");

        response.append("func H() { // Heaviside function, can be used to set gmax = 0 when x <100 etc.\n");
        response.append("    if ($1>=0) return 1\n");
        response.append("    return 0\n");
        response.append("}\n\n");
        
        response.append(postProcs.toString());
 
        return response.toString();
    }

    private String getProcPosition()
    {
        logger.logComment("calling getProcPosition");
        StringBuffer response = new StringBuffer();

        response.append("proc position() { local i\n");


        response.append("    forsec all {\n");
        response.append("        for i = 0, n3d()-1 {\n");
        
        response.append("            pt3dchange(i, $1+x3d(i), $2+y3d(i), $3+z3d(i), diam3d(i))\n");
        response.append("        }\n");        
        response.append("    }\n");
        response.append("    x = $1  y = $2  z = $3\n");
        response.append("}\n");

        response.append("\n");
        return response.toString();
    }




    private String getProcConnect2target()
    {
        logger.logComment("calling getProcConnect2target");
        StringBuffer response = new StringBuffer();
        response.append("proc connect2target() {   //$o1 target point process, $o2 returned NetCon\n\n");

         NeuronFileManager.addHocComment(response, "Using standard NetBuilder form. (Overly) simple assumption that first soma seg is trigger for AP...\nNote: neuroConstruct does not use this func for creating connections (in serial mode!), but it can be useful when using generated files in NEURON's NetBuilder");


         //response.append("    print \"connect2target() being called on cell: \", reference, \", post obj:\", $o1\n");
        response.append("    "+NeuronFileManager.getHocSectionName(cell.getFirstSomaSegment().getSection().getSectionName())
                +" $o2 = new NetCon(&v(1), $o1)\n");
        
        if (NeuronFileManager.addComments()) response.append("    print \"connect2target called on \", name\n");

        response.append("}\n");
        response.append("\n");
        return response.toString(); 
    }
    
    


    private String getSegIdFunctions()
    {
        logger.logComment("calling accessSectionForSegId");
        StringBuffer response = new StringBuffer();

        NeuronFileManager.addHocComment(response, "Accessing the section which corresponds to the given segment id");

        response.append("proc accessSectionForSegId() {   \n\n");

        response.append("    id = $1\n");
         
        Vector<Segment> segs = cell.getAllSegments();
        
        for(Segment seg: segs)
        {
            response.append("    if (id == "+seg.getSegmentId()+")  { access "+seg.getSection().getSectionName()+" }\n");
        }

        response.append("}\n");
        response.append("\n");
        
        
        
        logger.logComment("calling getFractAlongSection");

        NeuronFileManager.addHocComment(response, "For getting the fraction along the NEURON section, given the fraction\n"
                +"along the segment who's id is given\nNOTE:This function will produce incorrect results if the morphology of the cell is altered after initialisation\n"
                +"TODO: alter to use pt3d info direct from section, getting lengths from those... (may be slower)");

        response.append("func getFractAlongSection() {   \n\n");

        response.append("    fractionAlongSegment = $1\n");
        response.append("    id = $2\n");
         
        ArrayList<Section> secs = cell.getAllSections();
        
        for(Section sec: secs)
        {
            LinkedList<Segment> mySegs = cell.getAllSegmentsInSection(sec);
           
            response.append("    // Section "+sec.getSectionName()+" has "+mySegs.size()+" segment"+(mySegs.size()>1?"s":"")+"\n");
            
            float secLength = CellTopologyHelper.getSectionLength(cell, sec);
            float traversed = 0;
            
            if (mySegs.size()>1)
            {
                for(Segment mySeg: mySegs)
                {
                    response.append("    if (id == "+mySeg.getSegmentId()+")  { return (("+traversed+" + (fractionAlongSegment*"+mySeg.getSegmentLength()+"))/"+secLength+") }\n");
                    traversed = traversed+ mySeg.getSegmentLength();
                }
               
            }
            else if (mySegs.size()==1)
            {
                response.append("    if (id == "+mySegs.get(0).getSegmentId()+")  {return fractionAlongSegment} // one seg in sec, so return immediately\n");
            }
        }
        response.append("\n    return fractionAlongSegment // assumes id not found, i.e. a one segment section...\n");

        response.append("}\n");
        response.append("\n");
        
        
        return response.toString();
    }



/*
    private String getProcSynapses()
    {
        logger.logComment("calling getProcSynapses");
        StringBuffer response = new StringBuffer();
        response.append("proc synapses() {\n");

        response.append("//  Nothing for now...\n");
        response.append("}\n");
        response.append("\n");
        return response.toString();
    }*/




    public static void main(String[] args)
    {
        try
        {
            Project testProj = Project.loadProject(new File("models/BioMorph/BioMorph.neuro.xml"),
                                                   new ProjectEventListener()
            {
                public void tableDataModelUpdated(String tableModelName)
                {};

                public void tabUpdated(String tabName)
                {};
                public void cellMechanismUpdated()
                {
                };

            });

            //SimpleCell cell = new SimpleCell("DummyCell");
            //ComplexCell cell = new ComplexCell("DummyCell");

            Cell cell = testProj.cellManager.getCell("LongCellDelayLine");

            //File f = new File("/home/padraig/temp/tempNC/NEURON/PatTest/basics/");
            File f = new File("../temp");

            NeuronTemplateGenerator cellTemplateGenerator1 = new NeuronTemplateGenerator(testProj, cell,
                f, false, false);

            System.out.println("Generated: " + cellTemplateGenerator1.generateFile());
            System.out.println(CellTopologyHelper.printDetails(cell, null));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
