package com.akaitigo.labeldecode.repository

import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import javax.sql.DataSource

@ApplicationScoped
open class AdditiveMasterRepository(
    private val dataSource: DataSource,
) {
    open fun findCategoryByName(name: String): String? =
        executeQuery(
            "SELECT category FROM additive_master WHERE name = ?",
            name,
        )

    open fun findCategoryByPartialName(name: String): String? =
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
        val rs = prepareAndExecute(sql, param)
        return rs.use { extractCategory(it) }
    }

    private fun prepareAndExecute(
        sql: String,
        param: String,
    ): ResultSet {
        val conn = dataSource.connection
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, param)
        return stmt.executeQuery()
    }

    private fun extractCategory(rs: ResultSet): String? =
        if (rs.next()) {
            rs.getString("category")
        } else {
            null
        }
}
