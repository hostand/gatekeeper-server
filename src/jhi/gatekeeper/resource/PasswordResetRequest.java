package jhi.gatekeeper.resource;

import java.io.*;

/**
 * @author Sebastian Raubach
 */
public class PasswordResetRequest implements Serializable
{
	private String username;
	private String email;

	public PasswordResetRequest()
	{
	}

	public PasswordResetRequest(String username, String email)
	{
		this.username = username;
		this.email = email;
	}

	public String getUsername()
	{
		return username;
	}

	public PasswordResetRequest setUsername(String username)
	{
		this.username = username;
		return this;
	}

	public String getEmail()
	{
		return email;
	}

	public PasswordResetRequest setEmail(String email)
	{
		this.email = email;
		return this;
	}
}
