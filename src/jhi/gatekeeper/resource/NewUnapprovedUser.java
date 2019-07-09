package jhi.gatekeeper.resource;

import jhi.gatekeeper.server.database.tables.pojos.*;

/**
 * @author Sebastian Raubach
 */
public class NewUnapprovedUser extends LocaleRequest
{
	private Integer id;
	private String  userUsername;
	private String  userPassword;
	private String  userFullName;
	private String  userEmailAddress;
	private Integer institutionId;
	private String  institutionName;
	private String  institutionAcronym;
	private String  institutionAddress;
	private Integer databaseSystemId;
	private Byte    needsApproval;

	public NewUnapprovedUser()
	{
	}

	public NewUnapprovedUser(UnapprovedUsers user)
	{
		this.id = user.getId();
		this.userUsername = user.getUserUsername();
		this.userPassword = user.getUserPassword();
		this.userFullName = user.getUserFullName();
		this.userEmailAddress = user.getUserEmailAddress();
		this.institutionId = user.getInstitutionId();
		this.institutionName = user.getInstitutionName();
		this.institutionAcronym = user.getInstitutionAcronym();
		this.institutionAddress = user.getInstitutionAddress();
		this.databaseSystemId = user.getDatabaseSystemId();
		this.needsApproval = user.getNeedsApproval();
	}

	public UnapprovedUsers getUnapprovedUser()
	{
		UnapprovedUsers user = new UnapprovedUsers();
		user.setId(id);
		user.setUserUsername(userUsername);
		user.setUserPassword(userPassword);
		user.setUserFullName(userFullName);
		user.setUserEmailAddress(userEmailAddress);
		user.setInstitutionId(institutionId);
		user.setInstitutionName(institutionName);
		user.setInstitutionAcronym(institutionAcronym);
		user.setInstitutionAddress(institutionAddress);
		user.setDatabaseSystemId(databaseSystemId);
		user.setNeedsApproval(needsApproval);

		return user;
	}

	public Integer getId()
	{
		return id;
	}

	public NewUnapprovedUser setId(Integer id)
	{
		this.id = id;
		return this;
	}

	public String getUserUsername()
	{
		return userUsername;
	}

	public NewUnapprovedUser setUserUsername(String userUsername)
	{
		this.userUsername = userUsername;
		return this;
	}

	public String getUserPassword()
	{
		return userPassword;
	}

	public NewUnapprovedUser setUserPassword(String userPassword)
	{
		this.userPassword = userPassword;
		return this;
	}

	public String getUserFullName()
	{
		return userFullName;
	}

	public NewUnapprovedUser setUserFullName(String userFullName)
	{
		this.userFullName = userFullName;
		return this;
	}

	public String getUserEmailAddress()
	{
		return userEmailAddress;
	}

	public NewUnapprovedUser setUserEmailAddress(String userEmailAddress)
	{
		this.userEmailAddress = userEmailAddress;
		return this;
	}

	public Integer getInstitutionId()
	{
		return institutionId;
	}

	public NewUnapprovedUser setInstitutionId(Integer institutionId)
	{
		this.institutionId = institutionId;
		return this;
	}

	public String getInstitutionName()
	{
		return institutionName;
	}

	public NewUnapprovedUser setInstitutionName(String institutionName)
	{
		this.institutionName = institutionName;
		return this;
	}

	public String getInstitutionAcronym()
	{
		return institutionAcronym;
	}

	public NewUnapprovedUser setInstitutionAcronym(String institutionAcronym)
	{
		this.institutionAcronym = institutionAcronym;
		return this;
	}

	public String getInstitutionAddress()
	{
		return institutionAddress;
	}

	public NewUnapprovedUser setInstitutionAddress(String institutionAddress)
	{
		this.institutionAddress = institutionAddress;
		return this;
	}

	public Integer getDatabaseSystemId()
	{
		return databaseSystemId;
	}

	public NewUnapprovedUser setDatabaseSystemId(Integer databaseSystemId)
	{
		this.databaseSystemId = databaseSystemId;
		return this;
	}

	public Byte getNeedsApproval()
	{
		return needsApproval;
	}

	public NewUnapprovedUser setNeedsApproval(Byte needsApproval)
	{
		this.needsApproval = needsApproval;
		return this;
	}
}
