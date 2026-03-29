package com.akaitigo.labeldecode.repository

import jakarta.enterprise.context.ApplicationScoped
import javax.sql.DataSource

@ApplicationScoped
class AdditiveMasterRepository(
    private val dataSource: DataSource,
) : AdditiveLookup {
    override fun findCategoryByName(name: String): String? =
        executeQuery(
            "SELECT category FROM additive_master WHERE name = ?",
            name,
        )

    override fun findCategoryByPartialName(name: String): String? =
        executeQuery(
            "SELECT category FROM additive_master " +
                "WHERE ? LIKE '%' || name || '%' " +
                "ORDER BY LENGTH(name) DESC LIMIT 1",
            name,
        )

    private fun executeQuery(
        sql: String,
        param: String,
    ): String? =
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, param)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getString("category")
                    } else {
                        null
                    }
                }
            }
        }
}
