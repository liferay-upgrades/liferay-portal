package ${package}.internal.servlet;

import ${package}.configuration.${className}APIConfiguration;
import ${package}.internal.${className}APIMockResponseUtil;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

#if (${jakartaCompatible.equals("true")})
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

#end
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Map;

#if (!${jakartaCompatible.equals("true")})
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

#end
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * @author ${author}
 */
@Component(
	configurationPid = "${package}.configuration.${className}APIConfiguration",
	property = {
		"osgi.http.whiteboard.context.path=/${artifactId}",
		"osgi.http.whiteboard.servlet.name=${className} API Mock Servlet",
		"osgi.http.whiteboard.servlet.pattern=/*"
	},
	service = Servlet.class
)
public class ${className}APIMockServlet extends HttpServlet {

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_configuration = ConfigurableUtil.createConfigurable(
			${className}APIConfiguration.class, properties);
	}

	@Override
	protected void doGet(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		httpServletResponse.setContentType("application/json; charset=UTF-8");

		if (!_configuration.mockEnabled()) {
			_writeError(
				httpServletResponse,
				HttpServletResponse.SC_SERVICE_UNAVAILABLE,
				"Mock responses are disabled");

			return;
		}

		if (!_isAuthorized(httpServletRequest)) {
			_writeError(
				httpServletResponse, HttpServletResponse.SC_UNAUTHORIZED,
				"Missing or invalid API token");

			return;
		}

		String mockResponse = ${className}APIMockResponseUtil.getMockResponse(
			httpServletRequest.getPathInfo());

		if (mockResponse == null) {
			_writeError(
				httpServletResponse, HttpServletResponse.SC_NOT_FOUND,
				"No mock response is bundled for " +
					httpServletRequest.getRequestURI());

			return;
		}

		httpServletResponse.setStatus(HttpServletResponse.SC_OK);

		PrintWriter printWriter = httpServletResponse.getWriter();

		printWriter.write(mockResponse);
	}

	private boolean _isAuthorized(HttpServletRequest httpServletRequest) {
		String token = _configuration.token();

		if (Validator.isNull(token)) {
			return true;
		}

		String authorization = httpServletRequest.getHeader("Authorization");

		if (Validator.isNull(authorization)) {
			return false;
		}

		return token.equals(
			StringUtil.removeSubstring(authorization, "Bearer "));
	}

	private void _writeError(
			HttpServletResponse httpServletResponse, int status, String message)
		throws IOException {

		httpServletResponse.setStatus(status);

		PrintWriter printWriter = httpServletResponse.getWriter();

		printWriter.write(
			JSONUtil.put(
				"error", true
			).put(
				"message", message
			).put(
				"status", status
			).toString());
	}

	private volatile ${className}APIConfiguration _configuration;

}