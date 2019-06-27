package jhi.gatekeeper.resource;

import java.io.*;

/**
 * @author Sebastian Raubach
 */
public class EmailUpdate implements Serializable
{
	private String oldEmail;
	private String newEmail;

	public EmailUpdate()
	{
	}

	public String getOldEmail()
	{
		return oldEmail;
	}

	public EmailUpdate setOldEmail(String oldEmail)
	{
		this.oldEmail = oldEmail;
		return this;
	}

	public String getNewEmail()
	{
		return newEmail;
	}

	public EmailUpdate setNewEmail(String newEmail)
	{
		this.newEmail = newEmail;
		return this;
	}
}
