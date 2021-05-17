package com.example.jdbc.jdbcsample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.text.CaseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alnt.platform.core.classdef.domain.ClassDef;
import com.alnt.platform.core.classdef.domain.FieldDef;

@Component
public class QueryGenerator {

	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Autowired
	JdbcRepository repository;

	public void createQueries(String packageName, Long classDefId, Long fieldDefId) {

		List<String> classes = null;
		try {

			List<String> classList = getClassNamesFromPackage(packageName);

			classes = classList.stream().map(e -> e.replaceAll(".class", "")).collect(Collectors.toList());

			List<String> classDefQueries = new ArrayList<>();
			List<String> fieldDefQueries = new ArrayList<>();

			for (String className : classes) {

				Class<?> theClass = Class.forName(className);

				int mod = theClass.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {

					Object obj = theClass.getDeclaredConstructor().newInstance();

					Table tableAnnotation = obj.getClass().getAnnotation(Table.class);

					if (tableAnnotation != null) {

						System.out.println(theClass.getName());
						// TODO what it will be the db tabl Name / className in simpleName form

						// extId should be same as the buscatObj

						// String extId = repository.getExtId(ObjTableName);

						String extId = obj.getClass().getSimpleName().toString();

						// insert query for classdef


						ClassDef classDef = new ClassDef();

						String ObjTableName = obj.getClass().getSimpleName().toString();

						Table table = classDef.getClass().getAnnotation(Table.class);

						String tableName = table.name();

						Map<String, List<Map<String, Object>>> clsDefQueryColumnData = getMetadata(classDef, tableName);

						Map<String, List<Map<String, Object>>> clsqueryValueData = getValueclsDefMetadata(obj, extId,
								obj.getClass().getSimpleName().toString(), tableName, classDefId);
						classDefId++;

						classDefQueries.addAll(getInsertQuery(clsDefQueryColumnData, clsqueryValueData));

						FieldDef fieldDef = new FieldDef();


						table = fieldDef.getClass().getAnnotation(Table.class);

						tableName = table.name();

						Map<String, List<Map<String, Object>>> queryColumnData = getMetadata(fieldDef, tableName);

						// System.out.println(queryColumnData);

						Map<String, List<Map<String, Object>>> queryValueData = getValueMetadata(obj, fieldDefId,
								extId);

						fieldDefQueries.addAll(getInsertQuery(queryColumnData, queryValueData));

					}
				}
			}
			createFile("E:\\script\\queries\\classdef", classDefQueries);
			createFile("E:\\script\\queries\\fieldDef", fieldDefQueries);

		} catch (Exception e1) {
			System.out.println(e1.getCause());
			e1.printStackTrace();
		}
	}

	public static Map<String, List<Map<String, Object>>> getMetadata(Object obj, String tableName) {

		// Map<tablename,Map<ColumnName,ColumnAttributes
		Map<String, List<Map<String, Object>>> tabelDef = new HashMap<String, List<Map<String, Object>>>();

		List<Map<String, Object>> columnDef = new ArrayList<Map<String, Object>>();

		Class<?> classObj = obj.getClass();

		Set<Field> fields = org.reflections.ReflectionUtils.getAllFields(classObj);

		for (Field field : fields) {
			field.setAccessible(true);
			Map<String, Object> coumnAttrs = new HashMap<String, Object>();

			Column column = null;
			Annotation[] as = field.getAnnotations();

			if (as.length > 0) {
				for (Annotation a : as) {
					if (a.annotationType() != Transient.class) {
						if (a.annotationType() == Id.class) {

							coumnAttrs.put("nullable", Boolean.FALSE);

							coumnAttrs.put("name", field.getName().toUpperCase());

						} else if (a.annotationType() == Column.class) {

							column = field.getAnnotation(Column.class);

							coumnAttrs.put("nullable", column.nullable());

							coumnAttrs.put("name", column.name());

							// coumnAttrs.put("length", column.length());
						} /*
							 * else if (a.annotationType() == JoinColumn.class) {
							 * 
							 * JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
							 * 
							 * coumnAttrs.put("nullable", joinColumn.nullable());
							 * 
							 * coumnAttrs.put("name", joinColumn.name());
							 * 
							 * //coumnAttrs.put("length", joinColumn.length()); }
							 */

					}
				}
				if (!coumnAttrs.isEmpty()) {
					if (field.getType().getName().equals("boolean")) {

						coumnAttrs.put("type", Boolean.class.getName());

					} else {

						coumnAttrs.put("type", field.getType().getName());
					}
					columnDef.add(coumnAttrs);
				}
			}

		}
		tabelDef.put(tableName, columnDef);
		return tabelDef;
	}

	public static Map<String, List<Map<String, Object>>> getValueMetadata(Object obj, Long fieldDefId, String extId) {

		// Map<tablename,Map<ColumnName,ColumnAttributes
		Map<String, List<Map<String, Object>>> tabelDef = new HashMap<String, List<Map<String, Object>>>();

		List<Map<String, Object>> columnDef = new ArrayList<Map<String, Object>>();

		Class<?> classObj = obj.getClass();

		Set<Field> fields = org.reflections.ReflectionUtils.getAllFields(classObj);

		Table table = obj.getClass().getAnnotation(Table.class);
		String tableName = table.name();
		for (Field field : fields) {
			field.setAccessible(true);
			Map<String, Object> coumnAttrs = new HashMap<String, Object>();

			Column column = null;
			Annotation[] as = field.getAnnotations();

			if (as.length > 0) {
				for (Annotation a : as) {
					if (a.annotationType() == Column.class) {

						column = field.getAnnotation(Column.class);

						coumnAttrs.put("LABEL", WordUtils.capitalizeFully(column.name(), '_').replaceAll("_", " "));

						coumnAttrs.put("COLUMN_NAME", column.name());

						coumnAttrs.put("FIELD_NAME", CaseUtils.toCamelCase(
								WordUtils.capitalizeFully(column.name(), '_').replaceAll("_", " "), false));

						// coumnAttrs.put("length", column.length());

						coumnAttrs.put("ID", fieldDefId);

						coumnAttrs.put("CLASS_EXT_ID", extId);
						fieldDefId = fieldDefId + 1;
					}

					if (!coumnAttrs.isEmpty()) {
						if (field.getType().getName().equals("boolean")) {

							coumnAttrs.put("TYPE", Boolean.class.getName());

						} else {

							coumnAttrs.put("TYPE", field.getType().getName());
						}
						columnDef.add(coumnAttrs);
					}
				}
			}
		}
		tabelDef.put(tableName, columnDef);
		return tabelDef;
	}

	public static Map<String, List<Map<String, Object>>> getValueclsDefMetadata(Object obj, String extId,
			String buscatObj, String tableName, Long classDefId) {

		Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();

		List<Map<String, Object>> columnList = new ArrayList<Map<String, Object>>();

		LocalDateTime now = LocalDateTime.now();

		Table table = obj.getClass().getAnnotation(Table.class);

		String objtableName = table.name();

		Map<String, Object> coumnAttrs = new HashMap<String, Object>();

		coumnAttrs.put("CHANGED_ON", dtf.format(now));

		coumnAttrs.put("CREATED_ON", dtf.format(now));

		coumnAttrs.put("INT_STATUS", 0);

		coumnAttrs.put("BUS_OBJ_CAT", buscatObj);

		coumnAttrs.put("CLASS_NAME", obj.getClass().getName());

		coumnAttrs.put("EXT_ID", buscatObj);

		coumnAttrs.put("TABLE_NAME", objtableName);

		coumnAttrs.put("ID", classDefId);

		columnList.add(coumnAttrs);

		result.put(tableName, columnList);

		return result;
	}

	public static Object getValueByFieldType(String type) {

		if (type.equals(String.class.getName())) {
			return null;
		}

		if (type.equals(Boolean.class.getName())) {
			return null;
		}
		return null;
	}

	public static String getValueByFieldType(Object value) {

		if (value instanceof String) {
			return "\'" + value + "\'";
		}
		return value.toString();

	}

	private static List<String> getInsertQuery(Map<String, List<Map<String, Object>>> queryColumnData,
			Map<String, List<Map<String, Object>>> queryValueData) {

		List<String> queriesList = new ArrayList<>();
		try {
			Set<Entry<String, List<Map<String, Object>>>> columnEntrySet = queryColumnData.entrySet();

			Optional<String> tableName = columnEntrySet.stream().map(e -> e.getKey()).findFirst();

			List<Map<String, Object>> columnsDetails = queryColumnData.get(tableName.get());

			Map<String, String> mappedData = columnsDetails.stream()
					.collect(Collectors.toMap(s -> (String) s.get("name"), s -> (String) s.get("type")));

			List<String> columnNames = columnsDetails.stream().map(e -> String.valueOf(e.get("name")))
					.collect(Collectors.toList());

			Set<Entry<String, List<Map<String, Object>>>> queryValueDataSet = queryValueData.entrySet();

			Optional<String> qyerySetTableName = queryValueDataSet.stream().map(e -> e.getKey()).findFirst();

			List<Map<String, Object>> querySetValueDetails = queryValueData.get(qyerySetTableName.get());

			// System.out.println(columnNames);

			String commSeparatedColumnNames = String.join(",", columnNames);

			int totalSize = columnNames.size();

			for (Map<String, Object> map : querySetValueDetails) {
				StringBuilder columnBuilder = new StringBuilder(" INSERT INTO devhsc.").append(tableName.get())
						.append(" ( ").append(commSeparatedColumnNames).append(" ) VALUES  ");
				StringBuilder valueBuilder = new StringBuilder(" ( ");
				int count = 0;

				for (String colName : columnNames) {

					count++;

					Object value = null;
					boolean exist = map.containsKey(colName);
					if (!exist) {
						String type = mappedData.get(colName);
						value = getValueByFieldType(type);
					} else {
						value = getValueByFieldType(map.get(colName));
					}
					valueBuilder.append(value);

					if (totalSize != count) {

						valueBuilder.append(" , ");
					}

				}
				valueBuilder.append(" ); ");

				String query = columnBuilder.append(valueBuilder).toString();
				queriesList.add(query);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queriesList;
	}

	private static String getDbDataType(String type) {

		if (type.equals("java.lang.String")) {
			return "VARCHAR";
		}

		if (type.equals("java.lang.Integer")) {

			return "INT";
		}

		return null;
	}

	private static void createFile(String file, List<String> arrData) throws IOException {
		FileWriter writer = new FileWriter(file + ".sql");
		int size = arrData.size();
		for (int i = 0; i < size; i++) {
			String str = arrData.get(i).toString();
			writer.write(str);
			if (i < size - 1)
				writer.write("\n");
		}
		writer.close();
	}

	public static String getExtId(Connection connection) {

		String extId = null;

		try {
			PreparedStatement statement = connection
					.prepareStatement("select ext_id from devhsc.CLASS_DEF where TABLE_NAME=?");

			statement.setString(1, "UserData");

			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				// System.out.printf(resultSet.getString("ext_id"));
				extId = resultSet.getString("ext_id");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return extId;

	}

	private static Long getFiedlDefId(Connection connection, String extId) {

		Long id = null;

		try {
			PreparedStatement statement = connection.prepareStatement(
					"select (fd.id) from devhsc.FIELD_DEF fd where fd.class_ext_id=? order by fd.id desc ");

			statement.setString(1, extId);

			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				// System.out.print(resultSet.getLong("id"));
				id = resultSet.getLong("id");
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	private static Long getClassDefId(Connection connection) {

		Long id = null;

		try {
			PreparedStatement statement = connection
					.prepareStatement("select (fd.id) from devhsc.CLASS_DEF fd order by fd.id desc ");

			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				// System.out.print(resultSet.getLong("id"));
				id = resultSet.getLong("id");
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	public static List<String> getClassNamesFromPackage(String packageName) throws IOException {

		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL packageURL;
			ArrayList<String> names = new ArrayList<>();
			;

			packageName = packageName.replace(".", "/");
			packageURL = classLoader.getResource(packageName);

			if (packageURL.getProtocol().equals("jar")) {
				String jarFileName;
				JarFile jf;
				Enumeration<JarEntry> jarEntries;
				String entryName;

				// build jar file name, then loop through zipped entries
				jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
				jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
				// System.out.println(">" + jarFileName);
				jf = new JarFile(jarFileName);
				jarEntries = jf.entries();
				while (jarEntries.hasMoreElements()) {
					entryName = jarEntries.nextElement().getName();
					if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
						names.add(entryName);
					}
				}

				// loop through files in classpath
			}
			List<String> finalClassNames = names.stream().map(e -> e.replaceAll("/", ".")).collect(Collectors.toList());
			return finalClassNames;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void createClassDefQueries(String className, Long classDefId) {

		List<String> classes = null;
		try {

			Class<?> classDefName = Class.forName(className);
			className = className.concat(".class");
			List<String> classList = Arrays.asList(className);

			classes = classList.stream().map(e -> e.replaceAll(".class", "")).collect(Collectors.toList());

			List<String> classDefQueries = new ArrayList<>();

			for (String clsName : classes) {

				Class<?> theClass = Class.forName(clsName);

				int mod = theClass.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {

					Object obj = theClass.getDeclaredConstructor().newInstance();

					Table tableAnnotation = obj.getClass().getAnnotation(Table.class);

					if (tableAnnotation != null) {

						System.out.println(theClass.getName());
						// TODO what it will be the db tabl Name / className in simpleName form

						// extId should be same as the buscatObj

						// String extId = repository.getExtId(ObjTableName);

						String extId = obj.getClass().getSimpleName().toString();

						// insert query for classdef

						clsName.getClass();
						// UserData userData = new UserData();

						ClassDef classDef = new ClassDef();

						Table table = classDef.getClass().getAnnotation(Table.class);

						String tableName = table.name();

						Map<String, List<Map<String, Object>>> clsDefQueryColumnData = getMetadata(classDef, tableName);

						Map<String, List<Map<String, Object>>> clsqueryValueData = getValueclsDefMetadata(obj, extId,
								obj.getClass().getSimpleName().toString(), tableName, classDefId);
						classDefId++;

						classDefQueries.addAll(getInsertQuery(clsDefQueryColumnData, clsqueryValueData));
					}
				}

			}

			String basePath="E:\\script\\queries\\classDefQueries";
			String path = basePath.concat(File.separator).concat(classDefName.getSimpleName());
			 
			File file = new File(basePath);
			file.mkdirs();
			createFile(path, classDefQueries);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createFieldDefQueries(String className,Long fieldDefId) {

		List<String> classes = null;
		try {

			Class<?> classDefName = Class.forName(className);
			className = className.concat(".class");
			List<String> classList = Arrays.asList(className);

			classes = classList.stream().map(e -> e.replaceAll(".class", "")).collect(Collectors.toList());

			List<String> fieldDefQueries = new ArrayList<>();

			for (String clsName : classes) {

				Class<?> theClass = Class.forName(clsName);

				int mod = theClass.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {

					Object obj = theClass.getDeclaredConstructor().newInstance();

					Table tableAnnotation = obj.getClass().getAnnotation(Table.class);

					if (tableAnnotation != null) {


						String extId = obj.getClass().getSimpleName().toString();

						ClassDef classDef = new ClassDef();

						Table table = classDef.getClass().getAnnotation(Table.class);

						String tableName = table.name();

						FieldDef fieldDef = new FieldDef();

						// feth fielddeftype columns

						table = fieldDef.getClass().getAnnotation(Table.class);

						tableName = table.name();

						Map<String, List<Map<String, Object>>> queryColumnData = getMetadata(fieldDef, tableName);

						// System.out.println(queryColumnData);

						Map<String, List<Map<String, Object>>> queryValueData = getValueMetadata(obj, fieldDefId,
								extId);

						fieldDefQueries.addAll(getInsertQuery(queryColumnData, queryValueData));
					}
				}

			}

			String basePath="E:\\script\\queries\\fieldDefQueries";
			String path = basePath.concat(File.separator).concat(classDefName.getSimpleName());
			 
			File file = new File(basePath);
			file.mkdirs();
			createFile(path, fieldDefQueries);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}