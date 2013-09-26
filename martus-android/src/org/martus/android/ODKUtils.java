package org.martus.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Set;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.martus.common.FieldSpecCollection;
import org.martus.common.PoolOfReusableChoicesLists;
import org.martus.common.ReusableChoices;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinConstants;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.CustomDropDownFieldSpec;
import org.martus.common.fieldspec.DateFieldSpec;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.fieldspec.FieldType;
import org.odk.collect.android.logic.FormController;
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
	private static final String ODK_TAG_CONSTRAINT = "constraint";
	private static final String ODK_TAG_SINGLE_SELECT = "select1";
	private static final String ODK_ATTRIBUTE_APPEARANCE = "appearance";
	private static final String ODK_ATTRIBUTE_REF = "ref";
	private static final String ODK_ATTRIBUTE_ID = "id";
	private static final String DEFAULT_MINIMUM_DATE = "date('1900-01-01')";
	private static final String BLANK_DATE = "today()";
	public static final String STRING_TRUE = "odk_simulate_True";
	public static final String STRING_FALSE = "odk_simulate_False";
	private static ChoiceItem[] booleanChoices;
	private static final String DATE_FORMAT_MARTUS = "%Y-%m-%d";

	private static boolean isCompatibleField(FieldSpec field)  {
		if (field.getTag().equals(BulletinConstants.TAGENTRYDATE)) {
			//this is a special system entered field
			return false;
		}
		FieldType type = field.getType();
		if (type.isDropdown())  {
			CustomDropDownFieldSpec dropDownFieldSpec = (CustomDropDownFieldSpec) field;
			if (!dropDownFieldSpec.hasDataSource())
		   	    return true;
		}
		return type.isString() || type.isMultiline() || type.isDate() || type.isBoolean();
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
		if (type.getTypeName().equals("BOOLEAN"))
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
		    createITextSection(serializer, fields, context, specCollection);
		    createBindSection(serializer, fields, context);

		    serializer.endTag("", "model");
	        serializer.endTag("", "h:head");

		    createBodySection(serializer, fields, context, specCollection);
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
				} else if (field.getType().isBoolean()) {
					serializer.text(STRING_FALSE);
				}
				serializer.endTag("", field.getTag());
			}
		}
		serializer.endTag("", "data");
		serializer.endTag("", "instance");
	}

	private static void createITextSection(XmlSerializer serializer, FieldSpec[] fields, Context context, FieldSpecCollection fieldCollection) throws IOException
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
		            CustomDropDownFieldSpec dropdownSpec = (CustomDropDownFieldSpec)field;
		            ChoiceItem[] choices = dropdownSpec.getAllChoices();
		            if (choices.length < 1) {
			            choices = getReusableChoices(fieldCollection, dropdownSpec);
		            }
		            createTextTagsFromChoices(serializer, field, choices);
	            } else if (field.getType().isBoolean()) {
		            ChoiceItem[] choices = getBooleanChoiceItems(context);
		            createTextTagsFromChoices(serializer, field, choices);
	            }
            }
		}
		serializer.endTag("", "translation");
		serializer.endTag("", "itext");
	}

	private static ChoiceItem[] getReusableChoices(FieldSpecCollection fieldCollection, CustomDropDownFieldSpec dropdownSpec)
	{
		ChoiceItem[] choices;PoolOfReusableChoicesLists choicesListPool =  fieldCollection.getAllReusableChoiceLists();
		CustomDropDownFieldSpec customDropDown = dropdownSpec;
		String[] choiceCodes = customDropDown.getReusableChoicesCodes();
		ReusableChoices newChoices = new ReusableChoices("", "");
		for (String code : choiceCodes) {
			ReusableChoices reusableChoices = choicesListPool.getChoices(code);
			newChoices.addAll(reusableChoices.getChoices());
		}
		choices = newChoices.getChoices();
		return choices;
	}

	private static ChoiceItem[] getBooleanChoiceItems(Context context)
	{
		if (booleanChoices == null) {
			booleanChoices = new ChoiceItem[2];
			booleanChoices[0] = new ChoiceItem(STRING_TRUE, context.getString(R.string.boolean_true));
			booleanChoices[1] = new ChoiceItem(STRING_FALSE, context.getString(R.string.boolean_false));
		}
		return booleanChoices;
	}

	private static void createTextTagsFromChoices(XmlSerializer serializer, FieldSpec field, ChoiceItem[] choices) throws IOException
	{
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

	private static void createBindSection(XmlSerializer serializer, FieldSpec[] fields, Context context) throws IOException
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

					String rawMinDate = DEFAULT_MINIMUM_DATE;
					String formattedMinDate = null;
					if (dateField.getMinimumDate() != null) {
						if (dateField.getMinimumDate().length() > 0) {
							rawMinDate =  dateField.getMinimumDate();
						} else {
							formattedMinDate =  BLANK_DATE;
						}
					}
					if (formattedMinDate == null) {
						formattedMinDate = formatDateForODK(rawMinDate);
					}

					String rawMaxDate = null;
					String formattedMaxDate = null;
					if (dateField.getMaximumDate() != null) {
						if (dateField.getMaximumDate().length() > 0) {
							rawMaxDate = dateField.getMaximumDate();
							formattedMaxDate = formatDateForODK(rawMaxDate);
						} else {
							formattedMaxDate =  BLANK_DATE;
						}
					}
					if (formattedMaxDate != null)
						serializer.attribute("",  ODK_TAG_CONSTRAINT, "(. >= " + formattedMinDate + " and . <= " + formattedMaxDate + ")");
					else
						serializer.attribute("", ODK_TAG_CONSTRAINT, ". >= " + formattedMinDate);

					String stringMinDate = (formattedMinDate.equals(BLANK_DATE)) ? "today" : rawMinDate;

					String validationMessage = context.getString(R.string.date_validation_min, stringMinDate);
					if (formattedMaxDate != null) {
						String stringMaxDate = (formattedMaxDate.equals(BLANK_DATE)) ? "today" : rawMaxDate;
						validationMessage = context.getString(R.string.date_validation_min_max, stringMinDate, stringMaxDate);
					}
					serializer.attribute("", "jr:constraintMsg", validationMessage);
				}
	            serializer.endTag("", ODK_TAG_BIND);
			}
		}
	}

	private static String formatDateForODK(String dateString)
	{
		return "date('" + dateString + "')";
	}

	private static void createBodySection(XmlSerializer serializer, FieldSpec[] fields, Context context, FieldSpecCollection fieldCollection) throws IOException
	{
		serializer.startTag("", "h:body");
		for (FieldSpec field: fields) {
            if (isCompatibleField(field)) {
	            if (field.getType().isDropdown() || field.getType().isBoolean()) {
		            serializer.startTag("", ODK_TAG_SINGLE_SELECT);
		            if (field.getType().isDropdown())
		                serializer.attribute("", ODK_ATTRIBUTE_APPEARANCE, "minimal");
		            serializer.attribute("", ODK_ATTRIBUTE_REF, "/data/" + field.getTag());
		            serializer.startTag("", ODK_TAG_LABEL);
		            serializer.attribute("", ODK_ATTRIBUTE_REF, "jr:itext('/data/" + field.getTag() + ":label')");
		            serializer.endTag("", ODK_TAG_LABEL);
		            ChoiceItem[] choices;
		            if (field.getType().isDropdown()) {
			            CustomDropDownFieldSpec dropdownSpec = (CustomDropDownFieldSpec)field;
                        choices = dropdownSpec.getAllChoices();
			            if (choices.length < 1) {
                            choices = getReusableChoices(fieldCollection, dropdownSpec);
                        }
		            } else {
			            choices = getBooleanChoiceItems(context);
		            }
		            createItemTagsFromChoices(serializer, field, choices);
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

	private static void createItemTagsFromChoices(XmlSerializer serializer, FieldSpec field, ChoiceItem[] choices) throws IOException
	{
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
	}

	public static void populateBulletin(Bulletin bulletin, FormController formController)
		{
			FormIndex i = formController.getFormIndex();
			formController.jumpToIndex(FormIndex.createBeginningOfFormIndex());

			int event;
			while ((event =
			        formController.stepToNextEvent(FormController.STEP_INTO_GROUP)) != FormEntryController.EVENT_END_OF_FORM) {
			    if (event != FormEntryController.EVENT_QUESTION) {
			        continue;
			    } else {
			        IAnswerData answer = formController.getQuestionPrompt().getAnswerValue();
				    FormEntryPrompt questionPrompt = formController.getQuestionPrompt();
			        String questionID = formController.getQuestionPrompt().getQuestion().getTextID();
				    int dataType = questionPrompt.getDataType();
			        if (answer != null) {
				        String tag =  questionID.substring(6, questionID.length() - 6);
				        String value = answer.getDisplayText();
				        if (dataType == Constants.DATATYPE_DATE) {
					        value = DateUtils.format((Date) answer.getValue(), DATE_FORMAT_MARTUS);
				        } else if (dataType == Constants.DATATYPE_CHOICE) {
					        if (value.equals(ODKUtils.STRING_TRUE)) {
						        value = FieldSpec.TRUESTRING;
					        } else if (value.equals(ODKUtils.STRING_FALSE)) {
						        value = FieldSpec.FALSESTRING;
					        }
				        }
				        Log.w(AppConfig.LOG_LABEL, "setting value of " + value);
				        bulletin.set(tag, value);
			        }
			    }
			}
		}

}
