package com.elipcero.springintegrationdemo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("receiver")
public class HttpReceiver {
	
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody FileResponse fileInfo) {

		System.out.println("HttpReceiver.. " + fileInfo.getName());
		System.out.println("HttpReceiver.. " + fileInfo.getContent());
		
		return ResponseEntity.ok(fileInfo.getName());
	}
}
