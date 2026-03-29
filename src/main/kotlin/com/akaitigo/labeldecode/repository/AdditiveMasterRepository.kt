package com.akaitigo.labeldecode.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import javax.sql.DataSource

@ApplicationScoped
class AdditiveMasterRepository {
    @Inject
    lateinit var dataSource: DataSource

    fun findCategoryByName(name: String): String? =
        executeQuery(
            "SELECT category FROM additive_master WHERE name = ?",
            name,
        )

    fun findCategoryByPartialName(name: String): String? =
        executeQuery(
            "SELECT category FROM additive_master " +
                "WHERE ? LIKE '%' || name || '%' " +
                "ORDER BY LENGTH(name) DESC LIMIT 1",
            name,
        )

    private fun executeQuery(
        sql: String,
        param: String,
    ): String? {
        val conn = dataSource.connection
        conn.use {
            val stmt = it.prepareStatement(sql)
            stmt.use { ps ->
                ps.setString(1, param)
                val rs = ps.executeQuery()
                return if (rs.next()) {
                    rs.getString("category")
                } else {
                    null
                }
            }
        }
    }
}
