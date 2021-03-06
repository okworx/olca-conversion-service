# openLCA conversion service
This project provides a web-service for converting LCA process data sets. It is
a Java application using the [Grizzly HTTP Server](https://javaee.github.io/grizzly/)
and the import and export functions of the
[openLCA core modules](https://github.com/GreenDelta/olca-modules). Currently,
the conversion between the following data formats are supported:

* [x] EcoSpold 1
* [x] [EcoSpold 2](https://www.ecoinvent.org/data-provider/data-provider-toolkit/ecospold2/ecospold2.html)
* [x] [ILCD](http://eplca.jrc.ec.europa.eu/LCDN/developer.xhtml)
* [x] [JSON LD](https://github.com/GreenDelta/olca-schema)
* [x] SimaPro CSV (*currently only as source format of a data conversion)

## Installation
In order to install the application a Java Runtime Environment >= 8 needs to be
installed. Then, unzip the distribution package. It contains the application as
a single jar file, a basic web UI, and a configuration file. You can start
the application via the following command:

```bash
java -jar server.jar
```

On a Linux server you probably want to start the server as a
[background process](https://stackoverflow.com/questions/4797050/how-to-run-process-as-background-and-never-die)
from a shell. One solution to do this, is to use `nohup`:

```bash
nohup java -jar server.jar &
```

If your shell does not return the process ID (to kill the process later) you can
get it via:

```bash
echo $!
```

### Configuration
The default configuration file looks like this (without the comments):

```javascript
{
  // the application host
  "host": "localhost",

  // the port of the application
  "port": 80,

  // path to a folder which contains the reference data and a file cache 
  "workspace": "./workspace",

  // an optional path to a folder with the web UI
  "ui": "./web",
}
```

#### Reference systems
The conversion service can be configured to run with different reference
systems where a reference system contains a set of reference data like
elementary flows, quantities, units etc. and mapping files which are used in the
conversion. The data of the reference systems are stored under the directory
`refsystems` of the `workspace` folder. Each reference system is stored in a
sub-folder of the `refsystems` directory and is identified by the name of this
sub-folder. 

There is at least one `default` reference system which is created automatically
on server startup if it does not exist. When a conversion request does not
specify a reference system, the `default` reference system is taken in the
conversion. The data of a reference system are stored in a file `data.zip` in
the folder of the reference system. The content of the `data.zip` file should
be provided in the [openLCA JSON-LD format](https://github.com/GreenDelta/olca-schema)
as produced by the openLCA export. Additionally, a set of mapping files (see
below) can be provided in a sub-folder `mappings` in the data directory of the
reference system.

On server startup, a data dump for each reference system is created if it does
not exist yet. For each conversion against a reference system the dump of this
system is loaded into memory and used for the conversion. Thus, to update the
data of a reference system, just update the `data.zip` and mapping files,
delete the `dump` folder and restart the server.

```
- workspace
  - refsystems
     - default
       + dump
       + mappings
       - data.zip
     - myrefsystem
       + dump
       + mappings
       - data.zip 
```

#### Mapping files
In the `mappings` folder of a reference system, mapping files can be specified.
The mappings in these files are applied to the flows in the source format of
a conversion and are format specific:

* EcoSpold 1: `ecospold_flow_map.csv`
* EcoSpold 2: `ecospold_2_flow_map.csv`
* ILCD: `ilcd_flow_map.csv`
* SimaPro CSV: `sp_flow_import_map.csv`

The mappings in these files should map to the data in the `data.zip` that you
provide with the reference system (see above). For the EcoSpold 1+2 and ILCD
format, the mapping files are simple CSV files with the following columns:

```
0: UUID of the flow in the source format
1: UUID of the reference flow in the database
2: a conversion factor f: <amount reference> = f * <amount source>
``` 

As flows in the EcoSpold 1 format have no UUIDs, MD5 based UUIDs are calculated
from flow attributes in this case. The format of the SimaPro CSV mapping file
is specified [in the olca-modules](https://github.com/GreenDelta/olca-modules/blob/master/olca-io/REF_DATA.md#simapro-csv-flow-import-mapping)
repository. The folder [default_mappings](./default_mappings) contains the
mapping files that the conversion service uses by default.

Additionally, these mappings can be overwritten in each conversion request by
providing a list of flow mappings directly in the conversion request: ...

## How the conversion works
A client sends a conversion request to the conversion server which includes an
URL to the respective process data set, the format of this data set, and the
target format to which this data set should be converted. The conversion server
then fetches all the required data from the data server using a format specific
protocol. The converted data set with all associated resources (e.g. flow data
sets etc.) are cached on the conversion server and a link to this data set is
send back to the client:

![How it works](./conversion-service.png)

Note that the client could be itself the back-end of an HTTP server that sends
back the link to another client. The following data provider are currently
supported:

| Format | Data provider | Example URL |
|--------| -------------|------------ |
| EcoSpold 1 | Plain HTTP server providing the data set | [example from the Needs project](http://www.needs-project.org/needswebdb/scripts/download.php?fileid=4&type=xml) |
| EcoSpold 2 | Plain HTTP server providing the data set | |
| ILCD | [soda4LCA](https://bitbucket.org/okusche/soda4lca) | [example from the ELCD database](http://eplca.jrc.ec.europa.eu/ELCD3/resource/processes/1a7da06d-e8b7-4ff1-920c-209e9009dbe0) |
| JSON LD | [openLCA CS](http://www.openlca.org/collaboration-server/) | e.g. http://localhost:8080/ws/public/browse/gdelta/refdata/PROCESS/e33fb2ad-5db5-4ee7-9486-515fce6fd46d |


## Building from source
This is a standard [Maven](https://maven.apache.org/) project using the Kotlin
compiler as Maven plugin. Thus you need to have Maven installed in order to
compile the project. Also, it depends on the [openLCA core modules](https://github.com/GreenDelta/olca-modules)
which need to be installed first. For Windows, there is a build script
`build.bat` which generates the distribution package:

```bash
cd olca-conversion-service
build
```

This will create the distribution package as `olca-conv_<version+date>.zip` in
the `target` folder. To include the user interface, you need to also build it
from source in the `ui` folder. We use [Gulp](https://gulpjs.com/) as build tool
for that:

```bash
cd olca-conversion-service/ui
npm install
gulp
```

The build script will package the UI in the distribution package if it can find
it.


## API

### Conversion Request

* **URL**

  `/api/convert`

* **Method**

  `POST`

* **Data Parameters**
        
  An JSON object that describes the conversion, e.g.:

```javascript
  {
    // the URL to the source data set
    "url": "http://eplca.jrc.ec.europa.eu/ELCD3/...",
    
    // the format of the source data set
    // possible values: "EcoSpold 1", "JSON LD", or "ILCD"
    "sourceFormat": "ILCD",
    
    // the target format into which the data set should be converted
    "targetFormat": "EcoSpold 1"
  }
```

* **Success Response:**

  * **Code**: 200 <br />
    **Content:** The conversion result as JSON object:

```javscript
{
  // the name of the result package for getting all data set resources via
  // result request; see below
  "zipFile": "e.g. 123...2434_ECOSPOLD_1.zip",
  
  // the converted process data set
  "process": "xml or JSON",
  
  // the format of the conversion result; "EcoSpold 1", "JSON LD", or "ILCD"
  "format": "EcoSpold 1"
}
```

* **Error Response:**

  * **Code**: 501, Not Implemented <br />
    **Content:** Message about unimplemented data conversion or unknown format.
  
  * **Code**: 500, Internal Server Error <br />
      **Content:** conversion error


### Get Conversion Result

* **URL**

  `/api/result/:file`

* **Method**

  `GET`

* **URL Parameters**
        
  The file name of a conversion result, e.g. `"123...2434_ECOSPOLD_1.zip"`

* **Success Response:**

  * **Code**: 200 <br />
    **Content:** A zip file.

* **Error Response:**

  * **Code:** 404, Not Found <br />
    **Content:** `"File <name> does not exist."`
  

License
-------
Unless stated otherwise, all source code of the openLCA project is licensed 
under the [Mozilla Public License, v. 2.0](http://www.mozilla.org/MPL/2.0/). 
Please see the LICENSE.txt file in the root directory of the source code.
