package ${package}.internal;

import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.InputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ${author}
 */
public class ${className}APIMockResponseUtil {

	public static String getMockResponse(String path) throws IOException {
		if (Validator.isNull(path)) {
			return null;
		}

		Matcher matcher = _pathPattern.matcher(path);

		if (!matcher.matches()) {
			return null;
		}

		Class<?> clazz = ${className}APIMockResponseUtil.class;

		try (InputStream inputStream = clazz.getResourceAsStream(
				"/mock-responses" + path + ".json")) {

			if (inputStream == null) {
				return null;
			}

			return StringUtil.read(inputStream);
		}
	}

	private static final Pattern _pathPattern = Pattern.compile("/[\\w-]+");

}