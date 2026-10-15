package ${package}.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author ${author}
 */
@ExtendedObjectClassDefinition(category = "third-party")
@Meta.OCD(
	id = "${package}.configuration.${className}APIConfiguration",
	name = "${className} API Configuration"
)
public interface ${className}APIConfiguration {

	@Meta.AD(
		deflt = "https://api.example.com", name = "API Base URL",
		required = false
	)
	public String baseURL();

	@Meta.AD(
		deflt = "true", name = "Enable Mock Responses", required = false
	)
	public boolean mockEnabled();

	@Meta.AD(deflt = "10", name = "Timeout in Seconds", required = false)
	public int timeout();

	@Meta.AD(deflt = "", name = "API Token", required = false)
	public String token();

}