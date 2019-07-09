/*
 * Copyright 2017 Information and Computational Sciences,
 * The James Hutton Institute.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jhi.gatekeeper.server.util;

import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.*;

/**
 * @author Sebastian Raubach
 */
public class Email
{
	private static String server;
	private static String address;
	private static String username;
	private static String password;
	private static String port;

	public static void init(String server, String address, String username, String password, String port)
	{
		Email.server = server;
		Email.address = address;
		Email.username = username;
		Email.password = password;
		Email.port = port;
	}

	private static boolean settingsValid()
	{
		// Check if any of the properties doesn't exist
		if (server == null || address == null || username == null)
			return false;

		return !StringUtils.isEmpty(server, address, username);
	}

	public static void sendAdministratorNotification(Locale locale, DatabaseSystems system, boolean needsReview)
		throws EmailException
	{
		if (needsReview)
			send(address,
				I18n.getString(Locale.ENGLISH, I18n.EMAIL_TITLE_USER_REGISTRATION_ADMIN_NOTIFICATION),
				I18n.getString(Locale.ENGLISH, I18n.EMAIL_MESSAGE_USER_REGISTRATION_ADMIN_NOTIFICATION, system.getServerName() + " " + system.getSystemName()));
		else
			send(address,
				I18n.getString(Locale.ENGLISH, I18n.EMAIL_TITLE_USER_ACTIVATED_AUTOMATICALLY_ADMIN_NOTIFICATION),
				I18n.getString(Locale.ENGLISH, I18n.EMAIL_MESSAGE_USER_ACTIVATED_AUTOMATICALLY_ADMIN_NOTIFICATION, system.getServerName() + " " + system.getSystemName()));
	}

	public static void sendAdministratorNotification(Locale locale, DatabaseSystems system)
		throws EmailException
	{
		send(address,
			I18n.getString(Locale.ENGLISH, I18n.EMAIL_TITLE_USER_REGISTRATION_ADMIN_NOTIFICATION),
			I18n.getString(Locale.ENGLISH, I18n.EMAIL_MESSAGE_USER_REGISTRATION_ADMIN_NOTIFICATION, system.getServerName() + " " + system.getSystemName()));
	}

	public static void sendNewPassword(Locale locale, UsersRecord user, String password)
		throws EmailException
	{
		send(user.getEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_NEW_PASSWORD),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_NEW_PASSWORD, password));
	}

	public static void sendPasswordChangeInfo(Locale locale, Users user)
		throws EmailException
	{
		send(user.getEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_PASSWORD_CHANGE),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_PASSWORD_CHANGE));
	}

	public static void sendActivationConfirmation(Locale locale, Users user)
		throws EmailException
	{
		send(user.getEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_USER_ACTIVATED),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_ACTIVATED));
	}

	public static void sendAwaitingApproval(Locale locale, Users user)
		throws EmailException
	{
		send(user.getEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_USER_REQUEST_PENDING),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_PENDING));
	}

	public static void sendAwaitingApproval(Locale locale, UnapprovedUsers user)
		throws EmailException
	{
		send(user.getUserEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_USER_REQUEST_PENDING),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_PENDING));
	}

	public static void sendActivationPrompt(Locale locale, UnapprovedUsers user, String url)
		throws EmailException
	{
		send(user.getUserEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_USER_ACTIVATION_PROMPT),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_ACTIVATION_PROMPT, url));
	}

	public static void sendAccessRequestApproved(Locale locale, Users user, DatabaseSystems system)
		throws EmailException
	{
		send(user.getEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_USER_REQUEST_APPROVED),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_APPROVED, StringUtils.isEmpty(system.getDescription()) ? system.getSystemName() : system.getDescription()));
	}

	public static void sendAccessRequestRejected(Locale locale, Users user, String rejectionReason)
		throws EmailException
	{
		String message;
		if (!StringUtils.isEmpty(rejectionReason))
			message = I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED, I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED_REASON, rejectionReason));
		else
			message = I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED, I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED_NO_REASON));

		send(user.getEmailAddress(), I18n.getString(locale, I18n.EMAIL_TITLE_USER_REQUEST_REJECTED), message);
	}

	public static void sendUnapprovedUserApproved(Locale locale, UnapprovedUsersRecord user, DatabaseSystems system)
		throws EmailException
	{
		send(user.getUserEmailAddress(),
			I18n.getString(locale, I18n.EMAIL_TITLE_USER_REQUEST_APPROVED),
			I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_APPROVED, StringUtils.isEmpty(system.getDescription()) ? system.getSystemName() : system.getDescription()));
	}

	public static void sendUnapprovedUserRejected(Locale locale, UnapprovedUsersRecord user, String rejectionReason)
		throws EmailException
	{
		String message;
		if (!StringUtils.isEmpty(rejectionReason))
			message = I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED, I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED_REASON, rejectionReason));
		else
			message = I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED, I18n.getString(locale, I18n.EMAIL_MESSAGE_USER_REQUEST_REJECTED_NO_REASON));

		send(user.getUserEmailAddress(), I18n.getString(locale, I18n.EMAIL_TITLE_USER_REQUEST_REJECTED), message);
	}

	/**
	 * Sends an email
	 *
	 * @param to          The recipient
	 * @param subject     The email subject
	 * @param htmlMessage The email message (HTML formatted)
	 * @throws EmailException Thrown if sending the email fails
	 */
	public static void send(String to, String subject, String htmlMessage)
		throws EmailException
	{
		try
		{
			if (!settingsValid())
				throw new EmailException("Invalid email properties");

			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", server);
			props.put("mail.smtp.port", StringUtils.isEmpty(port) ? "587" : port);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

			Session session;

			if (StringUtils.isEmpty(password))
			{
				props.put("mail.smtp.auth", "false");
				session = Session.getInstance(props);
			}
			else
			{
				props.put("mail.smtp.auth", "true");
				session = Session.getInstance(props, new javax.mail.Authenticator()
				{
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(username, password);
					}
				});
			}

			Message message = new MimeMessage(session);
			message.setContent(htmlMessage, "text/html; charset=utf-8");
			message.setFrom(new InternetAddress(address));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(subject);

			/* Send the email */
			Transport.send(message);
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
			throw new EmailException(e);
		}
	}
}
