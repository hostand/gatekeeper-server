package jhi.gatekeeper.server.resource;

import jhi.gatekeeper.server.util.StringUtils;
import org.restlet.resource.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Sebastian Raubach
 */
public class PaginatedServerResource extends ServerResource
{
	public static final String PARAM_PAGE      = "page";
	public static final String PARAM_LIMIT     = "limit";
	public static final String PARAM_QUERY     = "query";
	public static final String PARAM_ASCENDING = "ascending";
	public static final String PARAM_ORDER_BY  = "orderBy";

	protected int     currentPage;
	protected int     pageSize;
	protected String  query;
	protected Boolean ascending;
	protected String  orderBy;

	@Override
	protected void doInit()
		throws ResourceException
	{
		super.doInit();

		try
		{
			this.currentPage = Integer.parseInt(getQueryValue(PARAM_PAGE));
		}
		catch (NullPointerException | NumberFormatException e)
		{
			this.currentPage = 0;
		}
		try
		{
			this.pageSize = Integer.parseInt(getQueryValue(PARAM_LIMIT));
		}
		catch (NullPointerException | NumberFormatException e)
		{
			this.pageSize = Integer.MAX_VALUE;
		}
		try
		{
			this.query = getQueryValue(PARAM_QUERY);
		}
		catch (NullPointerException e)
		{
			this.query = null;
		}
		try
		{
			this.orderBy = getQueryValue(PARAM_ORDER_BY);
		}
		catch (NullPointerException e)
		{
			this.orderBy = null;
		}
		try
		{
			int value = Integer.parseInt(getQueryValue(PARAM_ASCENDING));
			ascending = value == 1;
		}
		catch (NullPointerException | NumberFormatException e)
		{
			this.ascending = null;
		}
	}

	protected String getRequestAttributeAsString(String parameter)
	{
		try
		{
			return URLDecoder.decode(getRequestAttributes().get(parameter).toString(), StandardCharsets.UTF_8.name());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
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
