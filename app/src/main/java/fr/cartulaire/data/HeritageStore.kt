package fr.cartulaire.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.FileOutputStream

class HeritageStore(private val context: Context) {
    private val db: SQLiteDatabase = open()

    init {
        seedIfEmpty()
    }

    fun query(bounds: MapBounds, kinds: Set<SiteKind>, dept: String? = null): List<HeritageSite> {
        if (kinds.isEmpty()) return emptyList()
        val limit = if (dept != null) 2_500 else 400
        val kindNames = kinds.map { it.name }
        val placeholders = kindNames.joinToString(",") { "?" }
        val args = ArrayList<String>()
        args += bounds.south.toString()
        args += bounds.north.toString()
        args += bounds.west.toString()
        args += bounds.east.toString()
        args.addAll(kindNames)
        val deptClause = if (dept != null) {
            args += dept
            " AND dept = ?"
        } else {
            ""
        }
        val sql = """
            SELECT id, name, kind, lat, lon, subtitle, wikipedia, wikidata, start_date, dept, region
            FROM sites
            WHERE lat BETWEEN ? AND ?
              AND lon BETWEEN ? AND ?
              AND kind IN ($placeholders)
              $deptClause
            LIMIT $limit
        """.trimIndent()
        return readSites(sql, args.toTypedArray())
    }

    fun region(code: String): AdminArea? = runCatching {
        db.rawQuery(
            "SELECT code, name, south, west, north, east FROM regions WHERE code = ?",
            arrayOf(code),
        ).use { c ->
            if (!c.moveToFirst()) null
            else AdminArea(
                c.getString(0),
                c.getString(1),
                null,
                c.getDouble(2),
                c.getDouble(3),
                c.getDouble(4),
                c.getDouble(5),
            )
        }
    }.getOrNull()

    fun department(code: String): AdminArea? = runCatching {
        db.rawQuery(
            "SELECT code, name, region, south, west, north, east FROM departments WHERE code = ?",
            arrayOf(code),
        ).use { c ->
            if (!c.moveToFirst()) null
            else AdminArea(
                c.getString(0),
                c.getString(1),
                c.getString(2),
                c.getDouble(3),
                c.getDouble(4),
                c.getDouble(5),
                c.getDouble(6),
            )
        }
    }.getOrNull()

    fun allRegions(): List<AdminArea> {
        val out = ArrayList<AdminArea>()
        db.rawQuery(
            "SELECT code, name, south, west, north, east FROM regions ORDER BY name",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out += AdminArea(
                    c.getString(0), c.getString(1), null,
                    c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getDouble(5),
                )
            }
        }
        return out
    }

    fun departmentsOf(region: String): List<AdminArea> {
        val out = ArrayList<AdminArea>()
        db.rawQuery(
            "SELECT code, name, region, south, west, north, east FROM departments WHERE region = ? ORDER BY name",
            arrayOf(region),
        ).use { c ->
            while (c.moveToNext()) {
                out += AdminArea(
                    c.getString(0), c.getString(1), c.getString(2),
                    c.getDouble(3), c.getDouble(4), c.getDouble(5), c.getDouble(6),
                )
            }
        }
        return out
    }

    fun count(): Int {
        db.rawQuery("SELECT COUNT(*) FROM sites", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun countInDept(dept: String, kinds: Set<SiteKind>): Map<SiteKind, Int> {
        if (kinds.isEmpty()) return emptyMap()
        val placeholders = kinds.joinToString(",") { "?" }
        val args = arrayOf(dept) + kinds.map { it.name }
        val map = mutableMapOf<SiteKind, Int>()
        db.rawQuery(
            "SELECT kind, COUNT(*) FROM sites WHERE dept = ? AND kind IN ($placeholders) GROUP BY kind",
            args,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val kind = runCatching { SiteKind.valueOf(cursor.getString(0)) }.getOrNull() ?: continue
                map[kind] = cursor.getInt(1)
            }
        }
        return map
    }

    fun upsert(sites: Collection<HeritageSite>) {
        if (sites.isEmpty()) return
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                """
                INSERT OR REPLACE INTO sites
                (id, name, kind, lat, lon, subtitle, wikipedia, wikidata, start_date, dept, region)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            )
            sites.forEach { site ->
                stmt.clearBindings()
                stmt.bindString(1, site.id)
                stmt.bindString(2, site.name)
                stmt.bindString(3, site.kind.name)
                stmt.bindDouble(4, site.lat)
                stmt.bindDouble(5, site.lon)
                site.subtitle?.let { stmt.bindString(6, it) } ?: stmt.bindNull(6)
                site.wikipedia?.let { stmt.bindString(7, it) } ?: stmt.bindNull(7)
                site.wikidata?.let { stmt.bindString(8, it) } ?: stmt.bindNull(8)
                site.startDate?.let { stmt.bindString(9, it) } ?: stmt.bindNull(9)
                site.dept?.let { stmt.bindString(10, it) } ?: stmt.bindNull(10)
                site.region?.let { stmt.bindString(11, it) } ?: stmt.bindNull(11)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun readSites(sql: String, args: Array<String>): List<HeritageSite> {
        val out = ArrayList<HeritageSite>()
        db.rawQuery(sql, args).use { cursor ->
            val iId = cursor.getColumnIndexOrThrow("id")
            val iName = cursor.getColumnIndexOrThrow("name")
            val iKind = cursor.getColumnIndexOrThrow("kind")
            val iLat = cursor.getColumnIndexOrThrow("lat")
            val iLon = cursor.getColumnIndexOrThrow("lon")
            val iSub = cursor.getColumnIndexOrThrow("subtitle")
            val iWiki = cursor.getColumnIndexOrThrow("wikipedia")
            val iWd = cursor.getColumnIndexOrThrow("wikidata")
            val iDate = cursor.getColumnIndexOrThrow("start_date")
            val iDept = cursor.getColumnIndex("dept")
            val iReg = cursor.getColumnIndex("region")
            while (cursor.moveToNext()) {
                val kind = runCatching { SiteKind.valueOf(cursor.getString(iKind)) }.getOrNull()
                    ?: continue
                out += HeritageSite(
                    id = cursor.getString(iId),
                    name = cursor.getString(iName),
                    kind = kind,
                    lat = cursor.getDouble(iLat),
                    lon = cursor.getDouble(iLon),
                    subtitle = cursor.getString(iSub),
                    wikipedia = cursor.getString(iWiki),
                    wikidata = cursor.getString(iWd),
                    startDate = cursor.getString(iDate),
                    source = "local",
                    dept = if (iDept >= 0) cursor.getString(iDept) else null,
                    region = if (iReg >= 0) cursor.getString(iReg) else null,
                )
            }
        }
        return out
    }

    private fun seedIfEmpty() {
        if (count() < 1_000) upsert(SeedSites.all)
    }

    private fun open(): SQLiteDatabase {
        val file = context.getDatabasePath("heritage.db")
        file.parentFile?.mkdirs()
        val assetBytes = runCatching { context.assets.openFd("heritage.db").length }.getOrDefault(-1L)
        val shouldCopy = assetBytes > 0 && (!file.exists() || file.length() < assetBytes)
        if (shouldCopy) {
            runCatching {
                context.assets.open("heritage.db").use { input ->
                    FileOutputStream(file).use { input.copyTo(it) }
                }
            }
        }
        if (!file.exists()) {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { empty ->
                empty.execSQL(CREATE_SQL)
            }
        }
        val opened = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        val hasRegions = opened.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='regions'",
            null,
        ).use { it.moveToFirst() }
        if (!hasRegions && assetBytes > 0) {
            opened.close()
            file.delete()
            context.assets.open("heritage.db").use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }
            return SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        }
        return opened
    }

    companion object {
        private const val CREATE_SQL = """
            CREATE TABLE IF NOT EXISTS sites (
                id TEXT PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL,
                lat REAL NOT NULL, lon REAL NOT NULL, subtitle TEXT,
                wikipedia TEXT, wikidata TEXT, start_date TEXT,
                dept TEXT, region TEXT
            )
        """
    }
}
