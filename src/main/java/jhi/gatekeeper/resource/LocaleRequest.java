package jhi.gatekeeper.resource;

import java.util.Locale;

import jhi.gatekeeper.server.util.StringUtils;

/**
 * @author Sebastian Raubach
 */
public class LocaleRequest
{
	private String locale;

	public LocaleRequest()
	{
	}

	public LocaleRequest(String locale)
	{
		this.locale = locale;
	}

	public String getLocale()
	{
		return locale;
	}

	public void setLocale(String locale)
	{
		this.locale = locale;
	}

	public Locale getJavaLocale()
	{
		return StringUtils.isEmpty(locale) ? Locale.ENGLISH : Locale.forLanguageTag(locale.replace("_", "-"));
	}

	public void setJavaLocale(Locale locale)
	{
		this.locale = locale == null ? null : locale.toLanguageTag().replaceAll("-", "_");
	}
}
