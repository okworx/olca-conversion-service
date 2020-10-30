package app.model

import app.Server
import org.openlca.core.database.IDatabase
import org.openlca.core.database.ProcessDao
import org.openlca.io.xls.process.output.ExcelExport
import java.io.File

class ExportXLS : Export {

    override val format = Format.XLS

    private val cache = Server.cache!!

    override fun doIt(db: IDatabase): File {
        val dir = cache.tempDir()
        val exp = ExcelExport(dir, db, ProcessDao(db).descriptors)
        exp.run()
        return cache.zipFilesAndClean(dir)
    }

}