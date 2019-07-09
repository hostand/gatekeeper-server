package jhi.gatekeeper.resource;

import jhi.gatekeeper.server.database.tables.pojos.*;

/**
 * @author Sebastian Raubach
 */
public class NewAccessRequest extends LocaleRequest
{
	private Integer   id;
	private Integer   userId;
	private Integer   databaseSystemId;
	private Byte      needsApproval;

	public NewAccessRequest()
	{
	}

	public AccessRequests getAccessRequest()
	{
		AccessRequests request = new AccessRequests();
		request.setId(id);
		request.setUserId(userId);
		request.setDatabaseSystemId(databaseSystemId);
		request.setNeedsApproval(needsApproval);

		return request;
	}

	public Integer getId()
	{
		return id;
	}

	public NewAccessRequest setId(Integer id)
	{
		this.id = id;
		return this;
	}

	public Integer getUserId()
	{
		return userId;
	}

	public NewAccessRequest setUserId(Integer userId)
	{
		this.userId = userId;
		return this;
	}

	public Integer getDatabaseSystemId()
	{
		return databaseSystemId;
	}

	public NewAccessRequest setDatabaseSystemId(Integer databaseSystemId)
	{
		this.databaseSystemId = databaseSystemId;
		return this;
	}

	public Byte getNeedsApproval()
	{
		return needsApproval;
	}

	public NewAccessRequest setNeedsApproval(Byte needsApproval)
	{
		this.needsApproval = needsApproval;
		return this;
	}

	@Override
	public String toString()
	{
		return "NewAccessRequest{" +
			"id=" + id +
			", userId=" + userId +
			", databaseSystemId=" + databaseSystemId +
			", needsApproval=" + needsApproval +
			"} " + super.toString();
	}
}
