package com.example.jdbc.jdbcsample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	NamedParameterJdbcTemplate template;

	public String getExtId(String tableName) {

		String extid = jdbcTemplate.queryForObject("select ext_id from devhsc.CLASS_DEF where TABLE_NAME=?",
				new Object[] { tableName }, String.class);

		return extid;

	}

	public String findAll() {

		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("tableName", "UserData");

		return template.queryForObject("select ext_id from devhsc.CLASS_DEF where TABLE_NAME=:tableName", paramMap,
				String.class);

	}
	
	public Long getClassDefId() {

		Long id = null;
		try {
		Map<String, Object> paramMap = new HashMap<String, Object>();

		id=template.queryForObject("select max(fd.id) from devhsc.CLASS_DEF fd ", paramMap,
				Long.class);
		
		


		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}
	
	public  Long getFiedlDefId() {

		Long id = null;
		try {
		Map<String, Object> paramMap = new HashMap<String, Object>();

		id=template.queryForObject("select max(fd.id) from devhsc.FIELD_DEF fd", paramMap,
				Long.class);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
		
	}
}
