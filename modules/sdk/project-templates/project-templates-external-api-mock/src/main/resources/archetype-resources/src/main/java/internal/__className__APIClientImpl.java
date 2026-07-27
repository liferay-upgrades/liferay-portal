package ${package}.internal;

import ${package}.${className}APIClient;
import ${package}.configuration.${className}APIConfiguration;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;

import java.net.HttpURLConnection;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * @author ${author}
 */
@Component(
	configurationPid = "${package}.configuration.${className}APIConfiguration",
	service = ${className}APIClient.class
)
public class ${className}APIClientImpl implements ${className}APIClient {

	@Override
	public String get(String path) throws IOException, PortalException {
		if (_configuration.mockEnabled()) {
			return _getMockResponse(path);
		}

		return _getResponse(path);
	}

	@Override
	public JSONObject getJSONObject(String path)
		throws IOException, PortalException {

		return JSONFactoryUtil.createJSONObject(get(path));
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_configuration = ConfigurableUtil.createConfigurable(
			${className}APIConfiguration.class, properties);
	}

	private String _getMockResponse(String path)
		throws IOException, PortalException {

		String mockResponse =
			${className}APIMockResponseUtil.getMockResponse(path);

		if (mockResponse == null) {
			throw new PortalException("No mock response is bundled for " + path);
		}

		return mockResponse;
	}

	private String _getResponse(String path)
		throws IOException, PortalException {

		Http.Options options = new Http.Options();

		options.addHeader("Accept", "application/json");

		String token = _configuration.token();

		if (Validator.isNotNull(token)) {
			options.addHeader("Authorization", "Bearer " + token);
		}

		options.setLocation(_configuration.baseURL() + path);
		options.setTimeout(_configuration.timeout() * 1000);

		String content = HttpUtil.URLtoString(options);

		Http.Response response = options.getResponse();

		if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new PortalException(
				StringBundler.concat(
					"Response code ", response.getResponseCode(), " for ",
					options.getLocation(), ": ", content));
		}

		return content;
	}

	private volatile ${className}APIConfiguration _configuration;

}