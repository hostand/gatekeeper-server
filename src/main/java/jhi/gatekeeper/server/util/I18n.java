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

import java.text.MessageFormat;
import java.util.*;

/**
 * @author Sebastian Raubach
 */
public class I18n
{
	public static final String EMAIL_TITLE_USER_REGISTRATION_ADMIN_NOTIFICATION              = "email.title.user.registration.admin.notification";
	public static final String EMAIL_MESSAGE_USER_REGISTRATION_ADMIN_NOTIFICATION            = "email.message.user.registration.admin.notification";
	public static final String EMAIL_TITLE_USER_ACTIVATED_AUTOMATICALLY_ADMIN_NOTIFICATION   = "email.title.user.activated.automatically.admin.notification";
	public static final String EMAIL_MESSAGE_USER_ACTIVATED_AUTOMATICALLY_ADMIN_NOTIFICATION = "email.message.user.activated.automatically.admin.notification";
	public static final String EMAIL_TITLE_NEW_PASSWORD                                      = "email.title.new.password";
	public static final String EMAIL_MESSAGE_NEW_PASSWORD                                    = "email.message.new.password";
	public static final String EMAIL_TITLE_PASSWORD_CHANGE                                   = "email.title.password.change";
	public static final String EMAIL_MESSAGE_PASSWORD_CHANGE                                 = "email.message.password.change";
	public static final String EMAIL_TITLE_USER_ACTIVATED                                    = "email.title.user.activated";
	public static final String EMAIL_MESSAGE_USER_ACTIVATED                                  = "email.message.user.activated";
	public static final String EMAIL_TITLE_USER_REQUEST_PENDING                              = "email.title.user.request.pending";
	public static final String EMAIL_MESSAGE_USER_REQUEST_PENDING                            = "email.message.user.request.pending";
	public static final String EMAIL_TITLE_USER_ACTIVATION_PROMPT                            = "email.title.user.activation.prompt";
	public static final String EMAIL_MESSAGE_USER_ACTIVATION_PROMPT                          = "email.message.user.activation.prompt";
	public static final String EMAIL_TITLE_USER_REQUEST_APPROVED                             = "email.title.user.request.approved";
	public static final String EMAIL_MESSAGE_USER_REQUEST_APPROVED                           = "email.message.user.request.approved";
	public static final String EMAIL_TITLE_USER_REQUEST_REJECTED                             = "email.title.user.request.rejected";
	public static final String EMAIL_MESSAGE_USER_REQUEST_REJECTED                           = "email.message.user.request.rejected";
	public static final String EMAIL_MESSAGE_USER_REQUEST_REJECTED_NO_REASON                 = "email.message.user.request.rejected.no.reason";
	public static final String EMAIL_MESSAGE_USER_REQUEST_REJECTED_REASON                    = "email.message.user.request.rejected.reason";

	private static Map<Locale, ResourceBundle> BUNDLES = new HashMap<>();

	/**
	 * Returns the {@link String} from the {@link ResourceBundle} with optional parameter substitution using {@link MessageFormat#format(String,
	 * Object...)}.
	 *
	 * @param locale    The {@link Locale}
	 * @param key       The key of the resource
	 * @param arguments The arguments to substitute (optional)
	 * @return The {@link String} from the {@link ResourceBundle} with optionally substituted parameters
	 * @see ResourceBundle#getString(String)
	 * @see MessageFormat#format(String, Object...)
	 */
	public static String getString(Locale locale, String key, Object... arguments)
	{
		ResourceBundle bundle = getBundle(locale);

		String result = bundle.getString(key);
		result = MessageFormat.format(result, arguments);

		return result;
	}

	private static ResourceBundle getBundle(Locale locale)
	{
		Locale.setDefault(Locale.ENGLISH);

		if (locale == null)
			locale = Locale.ENGLISH;

		ResourceBundle bundle = BUNDLES.get(locale);

		if (bundle == null)
		{
			try
			{
				bundle = ResourceBundle.getBundle("Gatekeeper", locale);
				BUNDLES.put(locale, bundle);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				bundle = ResourceBundle.getBundle("Gatekeeper", Locale.ENGLISH);
			}
		}

		return bundle;
	}
}
