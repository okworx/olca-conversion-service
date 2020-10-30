package app.model

import app.Server
import org.openlca.core.database.IDatabase
import org.openlca.io.xls.process.input.ExcelImport
import org.slf4j.LoggerFactory
import java.net.URL

class ImportXLS : Import {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doIt(setup: ConversionSetup, db: IDatabase) {
        log.info("import XLS from {}", setup.url)
        val tempFile = Server.cache!!.tempFile(ext = ".xlsx")
        log.debug("copy data to {}", tempFile)
        URL(setup.url).openStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val imp = ExcelImport(tempFile, db)
        imp.run()
        log.debug("data imported; delete file {}", tempFile)
        tempFile.delete()
    }
}
