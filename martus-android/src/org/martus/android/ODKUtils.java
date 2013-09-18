package org.martus.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import org.martus.common.FieldSpecCollection;
import org.martus.common.bulletin.BulletinConstants;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.DateFieldSpec;
import org.martus.common.fieldspec.DropDownFieldSpec;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.fieldspec.FieldType;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

/**
 * @author roms
 *         Date: 9/16/13
 */
public class ODKUtils
{

	public static final String ODK_ROOT = Environment.getExternalStorageDirectory()
	            + File.separator + "odk";
    public static final String FORMS_PATH = ODK_ROOT + File.separator + "forms";

	private static final String ODK_TAG_LABEL = "label";
	private static final String ODK_TAG_VALUE = "value";
	private static final String ODK_TAG_ITEM = "item";
	private static final String ODK_TAG_TEXT = "text";
	private static final String ODK_TAG_BIND = "bind";
	private static final String ODK_TAG_INPUT = "input";
	private static final String ODK_TAG_SINGLE_SELECT = "select1";
	private static final String ODK_ATTRIBUTE_REF = "ref";
	private static final String ODK_ATTRIBUTE_ID = "id";

	private static boolean isCompatibleField(FieldSpec field)  {
		if (field.getTag().equals(BulletinConstants.TAGENTRYDATE)) {
			//this is a special system entered field
			return false;
		}
		FieldType type = field.getType();
		return type.isString() || type.isMultiline() || type.isDate() || type.isDropdown();
	}

	private static void addStandardLabels(Context context, FieldSpecCollection specCollection) {
		Set<FieldSpec> fields = specCollection.asSet();
		for (FieldSpec field: fields) {
			if (TextUtils.isEmpty(field.getLabel())) {
				field.setLabel(getStandardLocalizedLabel(context, field.getTag()));
			}
		}
	}

	private static String getODKType(FieldType type) {
		if (type.getTypeName().equals("STRING")  || type.getTypeName().equals("MULTILINE"))
			return "string";
		if (type.getTypeName().equals("DATE"))
			return "date";
		if (type.getTypeName().equals("DROPDOWN"))
			return "select1";
		return "";
	}

	private static String getStandardLocalizedLabel(Context context, String tag) {
		if (tag.equals(BulletinConstants.TAGAUTHOR)) {
			return context.getString(R.string.label_author);
		}
		if (tag.equals(BulletinConstants.TAGORGANIZATION)) {
			return context.getString(R.string.label_organization);
		}
		if (tag.equals(BulletinConstants.TAGTITLE)) {
			return context.getString(R.string.label_title);
		}
		if (tag.equals(BulletinConstants.TAGLOCATION)) {
			return context.getString(R.string.label_location);
		}
		if (tag.equals(BulletinConstants.TAGKEYWORDS)) {
			return context.getString(R.string.label_keywords);
		}
		if (tag.equals(BulletinConstants.TAGENTRYDATE)) {
			return context.getString(R.string.label_entrydate);
		}
		if (tag.equals(BulletinConstants.TAGSUMMARY)) {
			return context.getString(R.string.label_summary);
		}
		if (tag.equals(BulletinConstants.TAGPUBLICINFO)) {
			return context.getString(R.string.label_publicinfo);
		}

		return "";
	}

	public static String writeXml(Context context, FieldSpecCollection specCollection){

		// temp throwaway code to delete temp file used to pass odk xml to ODK app
		File dir = new File(FORMS_PATH);
		File file = new File(dir, "Martus.xml");
		file.delete();

		addStandardLabels(context, specCollection);
	    XmlSerializer serializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
		FieldSpec[] fields = specCollection.asArray();
	    try {
	        serializer.setOutput(writer);
	        serializer.startDocument("UTF-8", true);
	        serializer.startTag("", "h:html");
		    serializer.attribute("", "xmlns", "http://www.w3.org/2002/xforms");
		    serializer.attribute("", "xmlns:h", "http://www.w3.org/1999/xhtml");
		    serializer.attribute("", "xmlns:ev", "http://www.w3.org/2001/xml-events");
		    serializer.attribute("", "xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
		    serializer.attribute("", "xmlns:jr", "http://openrosa.org/javarosa");
		    serializer.startTag("","h:head");
		    serializer.startTag("", "h:title");
		    serializer.text("Martus");
		    serializer.endTag("", "h:title");
		    serializer.startTag("", "model");

		    createInstanceSection(serializer, fields);
		    createITextSection(serializer, fields);
		    createBindSection(serializer, fields);

		    serializer.endTag("", "model");
	        serializer.endTag("", "h:head");

		    createBodySection(serializer, fields);
	        serializer.endTag("", "h:html");
	        serializer.endDocument();

		    // temp throwaway code to pass odk xml to separate ODK app
		    try {
	            File outfile = new File(FORMS_PATH, "Martus.xml");
		            outfile.createNewFile();
	                FileOutputStream fos = new FileOutputStream(outfile);
	                fos.write(writer.toString().getBytes());
	                fos.flush();
	                fos.close();
	        } catch (Exception e) {
	            Log.e(AppConfig.LOG_LABEL, "problem writing odk xml file", e);
	        }

	        return writer.toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}

	private static void createInstanceSection(XmlSerializer serializer, FieldSpec[] fields) throws IOException
	{
		serializer.startTag("", "instance");
		serializer.startTag("", "data");
		serializer.attribute("", "id", "build_Martus");
		serializer.startTag("", "meta");
		serializer.startTag("", "instanceID");
		serializer.endTag("", "instanceID");
		serializer.endTag("", "meta");
		for (FieldSpec field: fields){
			if (isCompatibleField(field)) {
				serializer.startTag("", field.getTag());
				if (field.getDefaultValue() != null && !field.getType().isDate() && field.getDefaultValue().length() > 0) {
					serializer.text(field.getDefaultValue());
				}
				serializer.endTag("", field.getTag());
			}
		}
		serializer.endTag("", "data");
		serializer.endTag("", "instance");
	}

	private static void createITextSection(XmlSerializer serializer, FieldSpec[] fields) throws IOException
	{
		serializer.startTag("", "itext");
		serializer.startTag("", "translation");
		serializer.attribute("","lang", "eng");
		for (FieldSpec field: fields) {
            if (isCompatibleField(field)) {
                serializer.startTag("", ODK_TAG_TEXT);
                serializer.attribute("", ODK_ATTRIBUTE_ID, "/data/" + field.getTag()+":label");
                serializer.startTag("", ODK_TAG_VALUE);
                serializer.text(field.getLabel());
                serializer.endTag("", ODK_TAG_VALUE);
                serializer.endTag("", ODK_TAG_TEXT);
	            if (field.getType().isDropdown()) {
		            DropDownFieldSpec dropdownSpec = (DropDownFieldSpec)field;
		            ChoiceItem[] choices = dropdownSpec.getAllChoices();
		            int counter = 0;
		            for (ChoiceItem choice: choices) {
			            if (choice.getCode() != null && choice.getCode().length() > 0) {
				            serializer.startTag("", ODK_TAG_TEXT);
				            serializer.attribute("", ODK_ATTRIBUTE_ID, "/data/" + field.getTag()+":option" + counter++);
				            serializer.startTag("", ODK_TAG_VALUE);
				            serializer.text(choice.getLabel());
				            serializer.endTag("", ODK_TAG_VALUE);
				            serializer.endTag("", ODK_TAG_TEXT);
			            }
		            }
	            }
            }
		}
		serializer.endTag("", "translation");
		serializer.endTag("", "itext");
	}

	private static void createBindSection(XmlSerializer serializer, FieldSpec[] fields) throws IOException
	{
		serializer.startTag("", ODK_TAG_BIND);
		serializer.attribute("", "nodeset", "/data/meta/instanceID");
		serializer.attribute("", "type", "string");
		serializer.attribute("", "readonly", "true()");
		serializer.attribute("", "calculate", "concat('uuid:', uuid())");
		serializer.endTag("", ODK_TAG_BIND);
		for (FieldSpec field: fields) {
			if (isCompatibleField(field)) {
				serializer.startTag("", ODK_TAG_BIND);
				serializer.attribute("", "nodeset", "/data/" + field.getTag());
				serializer.attribute("", "type", getODKType(field.getType()));
				if (field.isRequiredField()) {
					serializer.attribute("", "required", "true()");
				}
				if (field.getType().isDate()) {
					DateFieldSpec dateField = (DateFieldSpec)field;
					boolean hasConstraint = false;
					Log.w(AppConfig.LOG_LABEL, "min date is " + dateField.getMinimumDate());
					if (dateField.getMinimumDate() != null && dateField.getMinimumDate().length() > 0) {
						hasConstraint = true;
						serializer.attribute("",  "constraint", ". >= today()");
						// constraint=". &gt;= today()" jr:constraintMsg="only future dates allowed"
						// constraint=". &gt;= today()"
					}
					Log.w(AppConfig.LOG_LABEL, "max date is " + dateField.getMaximumDate());
					if (hasConstraint) {
						serializer.attribute("", "jr:constraintMsg", "data validation failed");
					}
				}
				// constraint="(. &gt; '2013-09-01' and . &lt; '2013-09-16')" jr:constraintMsg="Value must be between 2013-09-01 and 2013-09-16"
				// constraint="(. &gt; '2000-01-05" jr:constraintMsg="data validation failed"
	            serializer.endTag("", ODK_TAG_BIND);
			}
		}
	}

	private static void createBodySection(XmlSerializer serializer, FieldSpec[] fields) throws IOException
	{
		serializer.startTag("", "h:body");
		for (FieldSpec field: fields) {
            if (isCompatibleField(field)) {
	            if (field.getType().isDropdown()) {
		            serializer.startTag("", ODK_TAG_SINGLE_SELECT);
		            serializer.attribute("", ODK_ATTRIBUTE_REF, "/data/" + field.getTag());
		            serializer.startTag("", ODK_TAG_LABEL);
		            serializer.attribute("", ODK_ATTRIBUTE_REF, "jr:itext('/data/" + field.getTag() + ":label')");
		            serializer.endTag("", ODK_TAG_LABEL);

		            DropDownFieldSpec dropdownSpec = (DropDownFieldSpec)field;
                    ChoiceItem[] choices = dropdownSpec.getAllChoices();
                    int counter = 0;
                    for (ChoiceItem choice: choices) {
	                    if (choice.getCode() != null && choice.getCode().length() > 0) {
				            serializer.startTag("", ODK_TAG_ITEM);
				            serializer.startTag("", ODK_TAG_LABEL);
				            serializer.attribute("", ODK_ATTRIBUTE_REF, "jr:itext('/data/" + field.getTag() + ":option" + counter + "')");
				            serializer.endTag("", ODK_TAG_LABEL);
		                    serializer.startTag("", ODK_TAG_VALUE);
		                    serializer.text(choice.getCode());
		                    serializer.endTag("", ODK_TAG_VALUE);
				            serializer.endTag("", ODK_TAG_ITEM);
		                    counter++;
	                    }
                    }
		            serializer.endTag("", ODK_TAG_SINGLE_SELECT);
	            } else {
		            serializer.startTag("", ODK_TAG_INPUT);
		            serializer.attribute("", ODK_ATTRIBUTE_REF, "/data/" + field.getTag());
		            serializer.startTag("", ODK_TAG_LABEL);
		            serializer.attribute("", ODK_ATTRIBUTE_REF, "jr:itext('/data/" + field.getTag() + ":label')");
		            serializer.endTag("", ODK_TAG_LABEL);
		            serializer.endTag("", ODK_TAG_INPUT);
	            }
            }
		}
		serializer.endTag("", "h:body");
	}

}
