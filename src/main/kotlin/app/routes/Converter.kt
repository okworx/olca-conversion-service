package app.routes

import app.Server
import app.model.ConversionResult
import app.model.ConversionSetup
import app.model.Export
import app.model.ExportEcoSpold1
import app.model.ExportEcoSpold2
import app.model.ExportILCD
import app.model.ExportJSON
import app.model.ExportXLS
import app.model.Format
import app.model.Import
import app.model.ImportEcoSpold1
import app.model.ImportEcoSpold2
import app.model.ImportILCD
import app.model.ImportJSON
import app.model.ImportXLS
import app.model.ImportSimaProCsv
import com.google.gson.Gson
import org.openlca.core.database.IDatabase
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

@Path("convert")
class Converter {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun convert(body: String): Response {
        val info = Gson().fromJson(body, ConversionSetup::class.java)
        val imp = getImport(info)
        if (imp == null) {
            val msg = "unsupported source format: ${info.sourceFormat}"
            return respond(msg, Status.NOT_IMPLEMENTED)
        }
        val exp = getExport(info)
        if (exp == null) {
            val msg = "unsupported target format: ${info.targetFormat}"
            return respond(msg, Status.NOT_IMPLEMENTED)
        }
        return doIt(info, imp, exp)
    }

    private fun doIt(setup: ConversionSetup, imp: Import, exp: Export): Response {
        var db: IDatabase? = null
        return try {
            val refSystem = Server.getRefSystem(setup.refSystem)
            db = refSystem.newDB()
            imp.doIt(setup, db)
            val zipFile = exp.doIt(db)
            val result = ConversionResult()
            result.zipFile = zipFile.name
            result.process = Server.cache!!.firstProcess(db, zipFile)
            result.format = exp.format.label
            Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build()
        } catch (e: Exception) {
            val msg = "Conversion failed: ${e.message}"
            respond(msg, Status.INTERNAL_SERVER_ERROR)
        } finally {
            db?.let {
                db.close()
                db.fileStorageLocation.deleteRecursively()
            }
        }
    }

    private fun respond(msg: String, stat: Status): Response {
        return Response.status(stat)
                .entity(msg)
                .type(MediaType.TEXT_PLAIN)
                .build()
    }

    private fun getImport(setup: ConversionSetup): Import? {
        val format = Format.get(setup.sourceFormat)
        return when(format) {
            Format.ILCD -> ImportILCD()
            Format.ECOSPOLD_1 -> ImportEcoSpold1()
            Format.ECOSPOLD_2 -> ImportEcoSpold2()
            Format.JSON_LD -> ImportJSON()
            Format.XLS -> ImportXLS()
            Format.SIMAPRO_CSV -> ImportSimaProCsv()
            else -> null
        }
    }

    private fun getExport(setup: ConversionSetup): Export? {
        val format = Format.get(setup.targetFormat)
        return when(format) {
            Format.JSON_LD -> ExportJSON()
            Format.ILCD -> ExportILCD()
            Format.ECOSPOLD_1 -> ExportEcoSpold1()
            Format.ECOSPOLD_2 -> ExportEcoSpold2()
            Format.XLS -> ExportXLS()
            else -> null
        }
    }
}
