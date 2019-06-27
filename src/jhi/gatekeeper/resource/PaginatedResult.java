package jhi.gatekeeper.resource;

import java.io.*;

/**
 * @author Sebastian Raubach
 */
public class PaginatedResult<T> implements Serializable
{
	private T data;
	private long count = 0;

	public PaginatedResult()
	{
	}

	public PaginatedResult(T data, long count)
	{
		this.data = data;
		this.count = count;
	}

	public T getData()
	{
		return data;
	}

	public PaginatedResult<T> setData(T data)
	{
		this.data = data;
		return this;
	}

	public long getCount()
	{
		return count;
	}

	public PaginatedResult<T> setCount(long count)
	{
		this.count = count;
		return this;
	}
}
