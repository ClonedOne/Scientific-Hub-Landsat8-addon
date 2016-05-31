package eu.esa.giorgio.landsat8;

/*
    Copyright (C) 2016 Giorgio Severi

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Landsat8Utils {

    private final static String METADATA_FILE_EXT = "_MTL.txt";
    private final static String DATE_ACQ = "DATE_ACQUIRED";
    private final static String CENTER_TIME = "SCENE_CENTER_TIME";
    private final static String WRONG_COORDS = "PROJECTION";
    private final static String COORDS_IDENTIFIER = "CORNER";
    private final static String DATE_FILE = "FILE_DATE";
    private final static String CLOUD_CVR = "CLOUD_COVER";
    private final static String INSTR = "SENSOR_ID";

    private final static String INSTR_NAME_COMB = "Combined Operational Land Imager and Thermal Infrared Sensor";
    private final static String INSTR_NAME_OLI= "Operational Land Imager";
    private final static String INSTR_NAME_TIRS = "Thermal Infrared Sensor";

    private final static int GML = 0;
    private final static int JTS = 1;
    private final static  int CHARS_TO_BE_REMOVED = 5;

    private static Logger logger = Logger.getLogger(Landsat8Utils.class);

    /*
    Required empty constructor
     */
    private Landsat8Utils(){}

    /*
    Acquires the file date from the metadata file. It has to compensate for the different date format.
    Sentinel products date has millisecond precision, Landsat 8 has hundredths of seconds precision.
     */
    @SuppressWarnings("unused")
    public static String acquireFileDate (Object currentFilePath, String metadataFileName){
        String date = "";
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath, metadataFileName));
        if(bufferedReader == null)
            return date;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                line = line.trim().replace("\"", "");
                String[] words = line.split(" ");
                if (words[0].equalsIgnoreCase(DATE_FILE)) {
                    date += words[2];
                    date = date.substring(0, date.length()-1);
                    date += ".000Z";
                }
                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired File Date = " + date);
        return date;
    }


    /*
    Acquires the sensing date and time from the metadata file. It has to compensate for the different date format.
    Sentinel products date has milliseconds precision, Landsat 8 has microseconds precision.
    Landsat 8 provides a single SCENE_CENTER_TIME attribute, instead of sensing start and stop times.
     */
    @SuppressWarnings("unused")
    public static String acquireSensingDate (Object currentFilePath, String metadataFileName){
        String date = "";
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath, metadataFileName));
        if(bufferedReader == null)
            return date;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                line = line.trim().replace("\"", "");
                String[] words = line.split(" ");
                if (words[0].equalsIgnoreCase(DATE_ACQ)) {
                    date += words[2];
                } else if (words[0].equalsIgnoreCase(CENTER_TIME)) {
                    date += "T" + words[2].substring(0, words[2].length() - CHARS_TO_BE_REMOVED) + "Z";
                }
                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired Date = " + date);
        return date;
    }


    /*
    Wrapper for the acquireFootprintString method, used for JTS formatting.
     */
    @SuppressWarnings("unused")
    public static String acquireFootprintStringJTS (Object currentFilePath, String metadataFileName){
        return acquireFootprintString(currentFilePath, metadataFileName, JTS);
    }


    /*
    Wrapper for the acquireFootprintString method, used for GML formatting.
     */
    @SuppressWarnings("unused")
    public static String acquireFootprintStringGML (Object currentFilePath, String metadataFileName){
        return acquireFootprintString(currentFilePath, metadataFileName, GML);
    }


    /*
    Search the metadata file for the coordinates of the footprint.
    First coordinate value in file is Latitude (latlong = false), second is Longitude (latlong = true).
    First point is replicated due to library necessities.
    --------------------------------------------------------------
    JTS Formatting.
    Returns a string containing points as couples of coordinates separated by ","; points are separated by " ".
    JTS requires Longitude before Latitude.
    --------------------------------------------------------------
    GML Formatting.
    Returns a string containing points as couples of coordinates separated by " "; points are separated by ",".
    GML requires Latitude before Longitude.
     */
    private static String acquireFootprintString (Object currentFilePath, String metadataFileName, int lib){
        String coords = "";
        String coordsArray[] = new String[5];
        boolean latlong = false;
        String latBuffer = "";
        String longBuffer;
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath, metadataFileName));
        if(bufferedReader == null)
            return coords;

        try {
            String line = bufferedReader.readLine();
            int i = 0;
            while (line != null) {
                String[] words = line.trim().split(" ");
                if (words[0].contains(COORDS_IDENTIFIER) && !words[0].contains(WRONG_COORDS)) {
                    if (!latlong) {
                        latBuffer = words[2];
                        latlong = true;
                    }else {
                        longBuffer  = words[2];
                        latlong = false;
                        if (lib == JTS)
                            coordsArray[i++] =  longBuffer + " " + latBuffer + ",";
                        else // lib == GML
                            coordsArray[i++] =  latBuffer + "," + longBuffer + " ";
                    }
                }

                line = bufferedReader.readLine();
            }

            coordsArray[4] = coordsArray[2];
            coordsArray[2] = coordsArray[3];
            coordsArray[3] = coordsArray[4];
            coordsArray[4] = coordsArray[0];

        } catch (IOException e){
            e.printStackTrace();
        }

        for (String point : coordsArray){
            coords += point;
        }
        coords = coords.substring(0, coords.length()-1);
        closeFile(bufferedReader);
        System.out.println("--------------- Acquired Coordinates = " + coords);
        logger.debug("--------------- Acquired Coordinates = " + coords);
        return coords;
    }


    /*
    Acquire cloud cover percentage from metadata file.
     */
    @SuppressWarnings("unused")
    public static String acquireCloudCover (Object currentFilePath, String metadataFileName){
        String cloudPct = "";
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath, metadataFileName));
        if(bufferedReader == null)
            return cloudPct;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                line = line.trim().replace("\"", "");
                String[] words = line.split(" ");
                if (words[0].equalsIgnoreCase(CLOUD_CVR)) {
                    cloudPct += words[2];
                }
                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired Cloud Cover Percentage = " + cloudPct);

        return cloudPct;
    }


    /*
    Acquire the instrument name from the file name.
    LC = Combine OLI and TIRS
    LO = OLI
    LT = TIRS
    Metadata file name and product file name both starts with the same character sequence.
     */
    @SuppressWarnings("unused")
    public static String acquireInstrumentName (String metadataFileName){
        String instrument = "";
        if (metadataFileName.startsWith("LC")){
            instrument = INSTR_NAME_COMB;
        } else if (metadataFileName.startsWith("LO")){
            instrument = INSTR_NAME_OLI;
        } else if (metadataFileName.startsWith("LT")){
            instrument = INSTR_NAME_TIRS;
        }
        logger.debug("--------------- Acquired Instrument long name = " + instrument);
        return instrument;
    }


    /*
    Acquires instrument short name from metadata file.
     */
    @SuppressWarnings("unused")
    public static String acquireInstrumentShortName (Object currentFilePath, String metadataFileName){
        String instrument = "";
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath, metadataFileName));
        if(bufferedReader == null)
            return instrument;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                line = line.trim().replace("\"", "");
                String[] words = line.split(" ");
                if (words[0].equalsIgnoreCase(INSTR)) {
                    instrument += words[2];
                }
                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired Instrument short name = " + instrument);
        return instrument;
    }


    /*
    Simple file management utilities
     */
    private static BufferedReader openFile (String filename){
        FileReader fileReader;
        BufferedReader bufferedReader = null;
        try{
            fileReader = new FileReader(filename);
            bufferedReader = new BufferedReader(fileReader);
        } catch (IOException e){
            e.printStackTrace();
        }
        return bufferedReader;
    }

    private static boolean closeFile(BufferedReader bufferedReader){
        try {
            bufferedReader.close();
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static String computeFilePath (Object currentFilePath, String metadataFileName){
        return currentFilePath.toString() + metadataFileName + METADATA_FILE_EXT;
    }


    /*
    Number formatting utilities
     */
    @SuppressWarnings("unused")
    public static String formatNumber(double value){
        logger.debug("----------Input value : "+value);
        return String.format("%.2f",value);
    }

}
