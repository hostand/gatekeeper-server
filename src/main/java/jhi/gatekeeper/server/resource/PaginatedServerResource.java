package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jhi.gatekeeper.server.util.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;

public abstract class PaginatedServerResource extends ContextResource
{
	@DefaultValue("-1")
	@QueryParam("prevCount")
	protected long previousCount;

	@DefaultValue("0")
	@QueryParam("page")
	protected int currentPage;

	@DefaultValue("2147483647")
	@QueryParam("limit")
	protected int pageSize;

	@QueryParam("ascending")
	private Integer isAscending;

	protected Boolean ascending = null;

	@QueryParam("orderBy")
	protected String orderBy;

	@QueryParam("query")
	protected String query;

	protected <T extends Record> SelectForUpdateStep<T> setPaginationAndOrderBy(SelectOrderByStep<T> step)
	{
		if (isAscending != null)
			this.ascending = this.isAscending == 1;

		if (ascending != null && orderBy != null)
		{
			if (ascending)
				step.orderBy(DSL.field(getSafeColumn(orderBy)).asc());
			else
				step.orderBy(DSL.field(getSafeColumn(orderBy)).desc());
		}

		return step.limit(pageSize)
				   .offset(pageSize * currentPage);
	}

	protected static String getSafeColumn(String column)
	{
		if (StringUtils.isEmpty(column))
		{
			return null;
		}
		else
		{
			return column.replaceAll("[^a-zA-Z0-9._-]", "").replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
		}
	}
}
