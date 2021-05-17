package com.example.jdbc.jdbcsample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequestMapping("/api/create")
public class BaseController {

	@Autowired
	QueryGenerator queryGenerator;
	
	@GetMapping(value="/queries", produces = MediaType.TEXT_PLAIN_VALUE)
	public void execute(@RequestParam String packageName, @RequestParam Long classDefId,@RequestParam Long fieldDefId) {
		
		queryGenerator.createQueries(packageName,classDefId,fieldDefId);
	}
	
	@GetMapping(value="/classDef", produces = MediaType.TEXT_PLAIN_VALUE)
	public void executeClassDef(@RequestParam String packageName, @RequestParam Long classDefId) {
		
		queryGenerator.createClassDefQueries(packageName,classDefId);
	}
	
	@GetMapping(value="/fieldDef", produces = MediaType.TEXT_PLAIN_VALUE)
	public void executeFieldDef(@RequestParam String packageName,@RequestParam Long fieldDefId) {
		
		queryGenerator.createFieldDefQueries(packageName,fieldDefId);
	}
}
