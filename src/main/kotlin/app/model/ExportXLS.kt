package app.model

import app.Server
import org.openlca.core.database.IDatabase
import org.openlca.core.database.ProcessDao
import org.openlca.io.xls.process.output.ExcelExport
import java.io.File
import org.slf4j.LoggerFactory

class ExportXLS : Export {

    private val log = LoggerFactory.getLogger(javaClass)

    override val format = Format.XLS

    private val cache = Server.cache!!

    override fun doIt(db: IDatabase): File {
        val dir = cache.tempDir()
  		log.debug("exporting as XLS using temp dir {}", dir)
        val exp = ExcelExport(dir, db, ProcessDao(db).descriptors)
        exp.run()
        return cache.zipFilesAndClean(dir)
    }

}