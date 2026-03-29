package com.akaitigo.labeldecode.parser

import com.akaitigo.labeldecode.repository.AdditiveMasterRepository
import javax.sql.DataSource

class StubAdditiveMasterRepository : AdditiveMasterRepository(StubDataSource()) {
    override fun findCategoryByName(name: String): String? = null

    override fun findCategoryByPartialName(name: String): String? = null
}

private class StubDataSource : DataSource {
    override fun getConnection() = throw UnsupportedOperationException()

    override fun getConnection(
        u: String?,
        p: String?,
    ) = throw UnsupportedOperationException()

    override fun getLogWriter() = throw UnsupportedOperationException()

    override fun setLogWriter(out: java.io.PrintWriter?) = Unit

    override fun setLoginTimeout(seconds: Int) = Unit

    override fun getLoginTimeout() = 0

    override fun getParentLogger() = throw UnsupportedOperationException()

    override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException()

    override fun isWrapperFor(iface: Class<*>?) = false

    override fun createConnectionBuilder() = throw UnsupportedOperationException()
}
