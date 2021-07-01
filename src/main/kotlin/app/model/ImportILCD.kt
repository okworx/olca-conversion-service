package app.model

import org.openlca.core.database.IDatabase
import org.openlca.ilcd.io.ZipStore
import org.openlca.io.ilcd.ILCDImport
import org.openlca.io.ilcd.input.ImportConfig
import app.Server
import org.slf4j.LoggerFactory
import java.net.URL
import java.io.File
import de.uba.probas2.util.RestPathUtil
import org.openlca.core.database.ProcessDao

class ImportILCD : Import {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doIt(setup: ConversionSetup, db: IDatabase) {
        val url = setup.url
        if (url.isEmpty() || !url.contains("/processes/"))
            throw Exception("Invalid URL: $url")
       
		// first import the UBA LCIA methods
		// apparently we can only get them in by means of a dummy process
        val lciamstore = ZipStore(File("UBA_LCIAmethods.zip"))
		
        val lciamconf = ImportConfig(lciamstore, db)
        lciamconf.flowMap = setup.flowMap()
        val lciamimp = ILCDImport(lciamconf)
        lciamimp.run()

		// the original URL looks like https://acme.org/resource/processes/f00b951c-e67f-4f7f-8dd4-665de3974e18?version=00.00.000
		val rpu = RestPathUtil(url)
  		val baseurl = rpu.baseUrl
		val uuid = rpu.uuid
		val versionParam = rpu.versionParam
		
		val zipurl = baseurl.plus("/processes/").plus(uuid).plus("/zipexport?").plus(versionParam)
		
		// instead of creating a SodaClient as DatasSource, we'll fetch
		// the dataset including its dependencies as ZIP
        log.debug("calling URL {}", zipurl)
        
		val tempFile = Server.cache!!.tempFile(ext = ".zip")
        log.debug("copy data to {}", tempFile)
        URL(zipurl).openStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

		val store = ZipStore(tempFile)
		
        val conf = ImportConfig(store, db)
        conf.flowMap = setup.flowMap()
        val imp = ILCDImport(conf)
        imp.run()
		
		// remove the dummy process so it won't get exported
		val pDao = ProcessDao(db)
		val dummy = pDao.getForRefId("00420000-c838-4b88-81cb-981aee9e8e2b")
		pDao.delete(dummy)

        log.debug("data imported; delete file {}", tempFile)
        tempFile.delete()
    }
}
