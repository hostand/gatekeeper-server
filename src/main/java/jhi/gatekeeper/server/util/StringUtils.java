package jhi.gatekeeper.server.util;

/**
 * @author Sebastian Raubach
 */
public class StringUtils
{
	/**
	 * Checks if the given {@link String} is either <code>null</code> or empty after calling {@link String#trim()}.
	 *
	 * @param input The {@link String} to check
	 * @return <code>true</code> if the given {@link String} is <code>null</code> or empty after calling {@link String#trim()}.
	 */
	public static boolean isEmpty(String input)
	{
		return input == null || input.trim().isEmpty();
	}

	/**
	 * Checks if the given {@link String}s are either <code>null</code> or empty after calling {@link String#trim()}.
	 *
	 * @param input The {@link String}s to check
	 * @return <code>true</code> if any of the given {@link String}s is <code>null</code> or empty after calling {@link String#trim()}.
	 */
	public static boolean isEmpty(String... input)
	{
		if (input == null)
			return true;
		for (String text : input)
		{
			if (StringUtils.isEmpty(text))
				return true;
		}

		return false;
	}
}
