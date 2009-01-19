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

package ucl.physiol.neuroconstruct.neuroml.hdf5;

import ncsa.hdf.object.*;
import ncsa.hdf.object.h5.*;
import java.io.*;
import java.util.*;

import java.util.ArrayList;
import javax.xml.parsers.*;

import ncsa.hdf.hdf5lib.HDF5Constants;
import org.xml.sax.*;

import ucl.physiol.neuroconstruct.dataset.DataSet;
import ucl.physiol.neuroconstruct.neuroml.*;
import ucl.physiol.neuroconstruct.project.*;
import ucl.physiol.neuroconstruct.utils.*;


/**
 * Utilities file for generating HDF5 files. Note this helper API is limited to the structures and conventions used in 
 * HDF5 files generated by neuroConstruct, and should only be used for reading/writing those!!
 *
 * @author Padraig Gleeson
 *
 */

public class Hdf5Utils
{
    private static ClassLogger logger = new ClassLogger("Hdf5Utils");
    
    public Hdf5Utils()
    {
        super();
    }
    
    /*
     * Helper function to throw error if no H5 classes found
     */
    private static void testH5classes() throws Hdf5Exception
    {
        try
        {
            ClassLoader cl  = ClassLoader.getSystemClassLoader();
            cl.loadClass("ncsa.hdf.object.FileFormat");
        }
        catch(Exception ex)
        {
            throw new Hdf5Exception("Problem finding HDF5 classes in classpath. Please ensure jars and libraries are installed correctly.\n", ex);
        }
    }
    
    public static Attribute getSimpleAttr(String name, String value, H5File h5File) throws Exception
    {
        Datatype dtype = h5File.createDatatype(Datatype.CLASS_STRING, value.length()+1, Datatype.NATIVE, Datatype.NATIVE);

        long[] attrDims = {1}; 

        Attribute attr = new Attribute(name, dtype, attrDims);
        String[] info = new String[]{value };

        attr.setValue(info); 
        
        return attr;
    }

    public static H5File createH5file(File file) throws Hdf5Exception
    {
        testH5classes();
        
        FileFormat fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);

        if (fileFormat == null)
        {
            throw new Hdf5Exception("Cannot find HDF5 FileFormat.");
        }

        try
        {
            H5File h5File = (H5File) fileFormat.create(file.getAbsolutePath());

            if (h5File == null)
            {
                throw new Hdf5Exception("Failed to create file:"+file);
            }

            return h5File;
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Error creating file: " + file + ".", ex);
        }
    }

    public static H5File openH5file(File file) throws Hdf5Exception
    {
        testH5classes();
        
        FileFormat fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);

        if (fileFormat == null)
        {
            throw new Hdf5Exception("Cannot find HDF5 FileFormat.");
        }

        try
        {
            H5File h5File = (H5File)fileFormat.open(file.getAbsolutePath(), FileFormat.READ);

            if (h5File == null)
            {
                throw new Hdf5Exception("Failed to open file:"+file);
            }
            
            logger.logComment("Opened HDF5 file: "+ file.getAbsolutePath()+ ", "+ h5File.getName());
            
            open(h5File);

            return h5File;
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Error creating file: " + file + ".", ex);
        }
    }

    public static void open(H5File h5File) throws Hdf5Exception
    {
        testH5classes();
        try
        {
            h5File.open();
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Failed to open HDF5 file", ex);
        }

    }

    public static H5File openForRead(File f) throws Hdf5Exception
    {
        try
        {
            H5File h5file = new H5File(f.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);
            
            h5file.open();
            
            return h5file;
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Failed to open HDF5 file", ex);
        }

    }

    public static void close(H5File h5File) throws Hdf5Exception
    {
        try
        {
            h5File.close();
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Failed to close HDF5 file", ex);
        }

    }
    
    
    public static String getFirstStringValAttr(ArrayList<Attribute> attrs, String attrName)
    {
        for (Attribute attribute : attrs) 
        {
            if (attribute.getName().equals(attrName))
            {
                if (attribute.getValue() instanceof Object[])
                {
                    Object[] vals = (Object[])attribute.getValue();

                    return vals[0].toString();
                }
                if (attribute.getValue() instanceof Object)
                {
                    System.out.println("--- "+attribute.getValue().toString()+" for "+ attrName);
                    //Object[] gg = (Object[])attribute.getValue();
                    
                    return attribute.getValue().toString();
                }
            }

        }
        return null;
    }
    
    public static ArrayList<Attribute> parseGroupForAttributes(Group g) throws Hdf5Exception
    {
        ArrayList<Attribute> attrs = new ArrayList<Attribute>();
        
        try
        {
            List attrList =  g.getMetadata();
            for(Object obj: attrList)
            {
                try
                {
                    Attribute a = (Attribute)obj;
                    attrs.add(a);
                }
                catch (ClassCastException ex)
                {
                    // Not an attribute...
                }
            }
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Failed to parse Group for Attributes", ex);
        }
        return attrs;
    }

    
    public static ArrayList<Attribute> parseDatasetForAttributes(Dataset d) throws Hdf5Exception
    {
        ArrayList<Attribute> attrs = new ArrayList<Attribute>();
        
        try
        {
            List attrList =  d.getMetadata();
            for(Object obj: attrList)
            {
                try
                {
                    Attribute a = (Attribute)obj;
                    attrs.add(a);
                }
                catch (ClassCastException ex)
                {
                    // Not an attribute...
                }
            }
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Failed to parse Dataset for Attributes", ex);
        }
        return attrs;
    }



    public static Group getRootGroup(H5File h5File) throws Hdf5Exception
    {
        Group root = (Group) ( (javax.swing.tree.DefaultMutableTreeNode) h5File.getRootNode()).getUserObject();

        if (root == null)
        {
            throw new Hdf5Exception("Failed to obtain root group of HDF5 file: "+ h5File.getFilePath());
        }

        return root;
    }
    
    public static String parseAttribute(Attribute a, String indent, Properties p)
    {
        String info = indent+"  Attribute name: "+ a.getName()+", value: ";
        
        String value = null;
        if (a.getValue() instanceof Object[])
        {
            for (Object m: (Object[])a.getValue())
            {
                if (((Object[])a.getValue()).length==1)
                    value = m.toString();
                else
                    value = value + "("+m.toString()+") ";
                    
                info = info + "("+m.toString()+") ";
            }
        }
        else if (a.getValue() instanceof int[])
        {
            for (int m: (int[])a.getValue())
            {
                if (((int[])a.getValue()).length==1)
                    value = m+"";
                else
                    value = value + "("+m+") ";
                
                info = info + "("+m  +")";
            }
        }
        else if (a.getValue() instanceof double[])
        {
            for (double m: (double[])a.getValue())
            {
                if (((double[])a.getValue()).length==1)
                    value = m+"";
                else
                    value = value + "("+m+") ";
                
                info = info + "("+m  +")";
            }
        }
        else
        {
            info = info + "(??? Class: "+a.getValue().getClass()+")";
        }
        
        p.setProperty(a.getName(), value);
        
        return info;
        
    }
    
       
    public static ArrayList<DataSet> parseGroupForDatasets(Group g, boolean includePoints) throws Exception
    {
        
        ArrayList<DataSet> dataSets = new ArrayList<DataSet>();
        
        if (g == null) return dataSets;
        
        logger.logComment("Searching group "+g.getFullName()+" for Datasets");

        java.util.List members = g.getMemberList();
             

        int n = members.size();
        
        HObject obj = null;
        
        for (int i=0; i<n; i++)
        {
            obj = (HObject)members.get(i);
            
            logger.logComment(obj+": "+ obj.getPath());
            
            java.util.List stuff = obj.getMetadata();
            
            Properties p = new Properties();
            
            for (Object m: stuff)
            {
                if (m instanceof Attribute)
                {
                    parseAttribute((Attribute)m, "", p);

                }
            }
            
            if (obj instanceof Group)
            {
                ArrayList<DataSet> childDataSets = parseGroupForDatasets((Group)obj, includePoints);
                dataSets.addAll(childDataSets);
            }
            
            if (obj instanceof Dataset)
            {
                Dataset d = (Dataset)obj;
                
                if (d.getDims().length==1)
                {
                    logger.logComment("Dimensions: "+d.getDims()[0]);
                    if (d.getDims()[0]>1)
                    {
                        DataSet ds = Hdf5Utils.parseDataset(d, includePoints, p);
                        ds.setRefrence(g.getFullName()+"/"+ds.getRefrence());
                        
                        logger.logComment(ds.toString());
                        String desc = ds.getDescription();
                        Enumeration propNames = p.propertyNames();
                        while (propNames.hasMoreElements())
                        {
                            String name = (String)propNames.nextElement();
                            desc = desc + "\n"+ name+" = "+ p.getProperty(name);
                        }
                        ds.setDescription(desc);
                        dataSets.add(ds);
                    }
                    
                }
                if (d.getDims().length==2)
                {
                    logger.logComment("Dimensions: "+d.getDims()[0]+", "+d.getDims()[1]);
                    logger.logComment("Ignoring...");
                }
                if (d.getDims().length==3)
                {
                    logger.logComment("Dimensions: "+d.getDims()[0]+", "+d.getDims()[1]+", "+d.getDims()[2]);
                    logger.logComment("Ignoring...");
                }
            }
        }
        return dataSets;
    }
    
    
    
    public static float[][] parse2Ddataset(Dataset d) throws Hdf5Exception
    {
        
        try
        {
            d.init();

            if (d.getDims().length!=2) return null;
            float[][] data = new float[(int)d.getDims()[0]][(int)d.getDims()[1]];
            
            float[] oneDdata = new float[(int)( d.getDims()[0] * d.getDims()[1]) ];

            Object dataObj = d.getData();
            logger.logComment("Got the data: "+ dataObj.toString()+ ", size: ("+d.getDims()[0]+", "+d.getDims()[1]+")");
            
            if (dataObj instanceof short[])
            {
                short[] datas = (short[])dataObj;

                logger.logComment("Got array of some shorts: "+ datas[0]);
                
                

                //oneDdata = datas;

            }
            else if (dataObj instanceof float[])
            {
                float[] dataf = (float[])dataObj;

                logger.logComment("Got array of some floats: "+ dataf[0]);
                
                oneDdata = dataf;
                

            }
            else if (dataObj instanceof double[])
            {
                double[] datad = (double[])dataObj;

                logger.logComment("Got array of some doubles: "+ datad[0]);
                
                

            }
            else
            {
                logger.logComment("Couldn't determine that datatype! ");
                return null;
            }
            
            for(int x=0;x<d.getDims()[0];x++)
            {
                for(int y=0;y<d.getDims()[1];y++)
                {
                    int oneD = (int)(x * d.getDims()[1]) + y;
                    
                    logger.logComment("(x,y)= ("+x+","+y+"), one D: "+oneD+", val: "+ oneDdata[oneD]);
                    
                    data[x][y] = oneDdata[oneD];
                }
            }
            

            return data;
        
        }
        catch (Exception ex)
        {
            throw new Hdf5Exception("Failed to parse2Ddataset", ex);
        }

                
    }
    
    public static DataSet parseDataset(Dataset d, boolean includePoints, Properties p)
    {
        String pre = "  -- ";
        String name = d.getName();
        StringBuffer desc = new StringBuffer(d.getName()+"\n");
        desc.append(d.getDatatype().getDatatypeDescription()+"\n");
        
        String xUnit = "";
        String xLegend = "x";
        String yUnit = "";
        String yLegend = "y";
        
        if (p.containsKey(Hdf5Constants.NEUROSAGE_TRACE_TYPE) &&
            p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_TYPE).equals(Hdf5Constants.NEUROSAGE_TRACE_TYPE_WAVEFORM))
        {
            xUnit = "s";
            xLegend = "Time";
        }
        
        double xSampling = 1;
        double yOffset = 0;
        double yScale = 1;
        
        if (p.containsKey(Hdf5Constants.NEUROSAGE_TRACE_SAMPLING_RATE))
        {
            xSampling = Double.parseDouble(p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_SAMPLING_RATE));
        }
        
        if (p.containsKey(Hdf5Constants.NEUROSAGE_TRACE_DATA_AXIS))
        {
            yLegend = p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_DATA_AXIS);
        }
        
        if (p.containsKey(Hdf5Constants.NEUROSAGE_TRACE_DATA_UNIT))
        {
            yUnit = p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_DATA_UNIT);
        }
        
        if (p.containsKey(Hdf5Constants.NEUROSAGE_TRACE_TRANSFORM_TYPE) &&
            p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_TRANSFORM_TYPE).equals(Hdf5Constants.NEUROSAGE_TRACE_TRANSFORM_TYPE_LINEAR))
        {
            yOffset = Double.parseDouble(p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_TRANSFORM_OFFSET));
            yScale = Double.parseDouble(p.getProperty(Hdf5Constants.NEUROSAGE_TRACE_TRANSFORM_SCALE));
        }
        
        
        
            logger.logComment(pre+""+d.getDatatype().getDatatypeDescription());
        
        long[] dims = d.getDims();
        
        
        DataSet ds = new DataSet(name, desc.toString(), xUnit, yUnit, xLegend, yLegend);
        
        for(int i=0;i<dims.length;i++)
        {
            String dimName = "";
            if (d.getDimNames()!=null) dimName = d.getDimNames()[i];
            
            logger.logComment(pre+"Dimension "+i+": " + dims[i]+", "+ dimName);
        }
        if (dims.length==1)
        {
            long size = dims[0];
            
            //if (d.getDatatype().)
            try
            {
                d.init();
                Object dataObj = d.getData();
                logger.logComment(pre+"Got the data: "+ dataObj.toString());
                
                if (includePoints)
                {
                    if (dataObj instanceof short[])
                    {
                        short[] data = (short[])dataObj;

                        logger.logComment(pre+"Got array of some: "+ data[0]);

                        for(int i=0;i<data.length;i++)
                        {
                            ds.addPoint(i/xSampling, yOffset + (yScale *data[i]));
                        }
                    }
                    else if (dataObj instanceof double[])
                    {
                        double[] data = (double[])dataObj;

                        logger.logComment(pre+"Got array of some: "+ data[0]);

                        for(int i=0;i<data.length;i++)
                        {
                            ds.addPoint(i/xSampling, yOffset + (yScale *data[i]));
                        }
                    }
                    else
                    {
                        logger.logComment(pre+"Couldn't determine that datatype! ");
                    }
                }
            }
            catch(Exception e)
            {
                logger.logComment(pre+"Couldn't read that datatype! ");
                e.printStackTrace();
            }
            
        }        
        
        return ds;
    }
    
    



    public static void main(String[] args)
    {
        String name = "TenMillionSyn";
        File h5File = new File("../temp/"+name+".h5");
        File newNMLFile = new File("../temp/"+name+".nml");
        try
        {
            System.setProperty("java.library.path", System.getProperty("java.library.path")+":/home/padraig/neuroConstruct");
            
            logger.logComment("Sys prop: "+System.getProperty("java.library.path"), true);
            
            Project testProj = Project.loadProject(new File("examples/Ex9-GranCellLayer/Ex9-GranCellLayer.neuro.xml"),
                                                   null);



            GeneratedCellPositions gcp = testProj.generatedCellPositions;
            GeneratedNetworkConnections gnc = testProj.generatedNetworkConnections;

            int sizeCells = 10000;
            int sizeConns = 10000000;
            String preGrp = "Mossies";
            String postGrp = "Grans";
            
            Random r = new Random();
            
            for(int i=0;i<sizeCells;i++)
            {
                gcp.addPosition(preGrp, new PositionRecord(i, 
                                                       r.nextFloat()*1000, 
                                                       r.nextFloat()*1000, 
                                                       r.nextFloat()*1000));
                
                gcp.addPosition(postGrp, new PositionRecord(i, 
                                                            r.nextFloat()*1000, 
                                                            r.nextFloat()*1000, 
                                                            r.nextFloat()*1000));
            }

            for(int i=0;i<sizeConns;i++)
            {
                int pre = r.nextInt(sizeCells);
                int post = r.nextInt(sizeCells);
                gnc.addSynapticConnection("NetConn_"+preGrp+"_"+postGrp, 
                                        GeneratedNetworkConnections.MORPH_NETWORK_CONNECTION, 
                                        pre, 
                                          0, 
                                          0.5f, 
                                          post, 
                                          0, 
                                          0.5f, 
                                          0, 
                                          null);
            }

            logger.logComment("Cells: " + gcp.getNumberInAllCellGroups(), true);
            logger.logComment("Net conn num: " + gnc.getNumberSynapticConnections(GeneratedNetworkConnections.ANY_NETWORK_CONNECTION), true);

            NetworkMLWriter.createNetworkMLH5file(h5File, 
                                                  testProj,
                                                  testProj.simConfigInfo.getDefaultSimConfig(),
                                                  NetworkMLConstants.UNITS_PHYSIOLOGICAL);
            
            if (true) System.exit(0);
            
            File fileSaved = null;
          

            fileSaved = ProjectManager.saveNetworkStructureXML(testProj, newNMLFile,
                                                      false,
                                                      false,
                                                      testProj.simConfigInfo.getDefaultSimConfig().getName(),
                                                               NetworkMLConstants.UNITS_PHYSIOLOGICAL);
     

            logger.logComment("File saved: " + fileSaved.getCanonicalPath(), true);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }


    }
}
