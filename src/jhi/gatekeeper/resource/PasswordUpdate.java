package jhi.gatekeeper.resource;

import java.io.*;

/**
 * @author Sebastian Raubach
 */
public class PasswordUpdate extends LocaleRequest implements Serializable
{
	private String oldPassword;
	private String newPassword;

	public PasswordUpdate()
	{
	}

	public String getOldPassword()
	{
		return oldPassword;
	}

	public PasswordUpdate setOldPassword(String oldPassword)
	{
		this.oldPassword = oldPassword;
		return this;
	}

	public String getNewPassword()
	{
		return newPassword;
	}

	public PasswordUpdate setNewPassword(String newPassword)
	{
		this.newPassword = newPassword;
		return this;
	}
}
