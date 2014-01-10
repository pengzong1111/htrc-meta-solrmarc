/*
#
# Copyright 2013 The Trustees of Indiana University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# -----------------------------------------------------------------
#
# Project: SolrMarc
# File:  HTRCIndexer.java
# Description: This indexer is the main indexer that HTRC uses to 
# index HTRC marc records and also other HTRC-contributed metadata
# fields. But this indexer is not responsible for indexing ocr. If
# you want to index OCR, please use the other indexer named 
# OCRIndexer in this same package
#
# -----------------------------------------------------------------
# 
*/
package org.solrmarc.htrc.index;

import gov.loc.repository.pairtree.Pairtree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.marc4j.marc.Record;
import org.solrmarc.index.SolrIndexer;
import org.solrmarc.tools.Utils;

import au.com.bytecode.opencsv.CSVReader;

public class HTRCIndexer extends SolrIndexer{

	private static final Map<String, String> volumeID2PageCountMap;
	private static final Map<String, String> volumeID2WordCountMap;
	private static final Map<String, String> volumeID2CharCountMap;
	private static final Map<String, String> authoName2GenderMap;
	//private static PrintWriter pw;
	private static final Pairtree pt = new Pairtree();
	
	private static int pageBinDivider1 = Integer.valueOf(HTRCConfigManager.getProperties("PAGE_BIN_DIVIDER_1"));
	private static int pageBinDivider2 = Integer.valueOf(HTRCConfigManager.getProperties("PAGE_BIN_DIVIDER_2"));
	private static int pageBinDivider3 = Integer.valueOf(HTRCConfigManager.getProperties("PAGE_BIN_DIVIDER_3"));
	
	private static int wordBinDivider1 = Integer.valueOf(HTRCConfigManager.getProperties("WORD_BIN_DIVIDER_1"));
	private static int wordBinDivider2 = Integer.valueOf(HTRCConfigManager.getProperties("WORD_BIN_DIVIDER_2"));
	private static int wordBinDivider3 = Integer.valueOf(HTRCConfigManager.getProperties("WORD_BIN_DIVIDER_3"));
	
	static{
		volumeID2PageCountMap = new HashMap<String, String>();
		volumeID2WordCountMap = new HashMap<String, String>();
		volumeID2CharCountMap = new HashMap<String, String>();
		authoName2GenderMap = new HashMap<String, String>();
		
		try {
		//	pw = new PrintWriter("debug.txt");
			initVolPageInfoMap(volumeID2PageCountMap);
			initVolWordAndCharCountMap(volumeID2WordCountMap, volumeID2CharCountMap);
			initAuthorName2GenderMap(authoName2GenderMap);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	// config initilization 
	 private static void initVolPageInfoMap(
				Map<String, String> volumeID2PageCountMap) throws IOException {

		File volumePageCount_txt = new File( HTRCConfigManager.getProperties("HTRC_VOLUME_PAGE_COUNT_FILE") );
			
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(volumePageCount_txt)));
			
		String line = null;
			
		while((line = br.readLine()) != null){
			
			String[] ID_PageCount = line.split("	");
			volumeID2PageCountMap.put(ID_PageCount[0], ID_PageCount[1]);
		}
			
		br.close();
	}

	private static void initAuthorName2GenderMap(
			Map<String, String> authoname2gendermap) {

		File authorNameGender_csv = new File( HTRCConfigManager.getProperties("HTRC_AUTHOR_GENDER_MAP_FILE") );
		
		try {
			
			CSVReader reader = new CSVReader(new FileReader(authorNameGender_csv));
			
			String [] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				
				if(nextLine[1].contains("female")){
					authoname2gendermap.put(nextLine[0], "female");
				}else if(nextLine[1].contains("male")){
					authoname2gendermap.put(nextLine[0], "male");
				}
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void initVolWordAndCharCountMap(
			Map<String, String> volumeid2wordcountmap,
			Map<String, String> volumeid2charcountmap) throws IOException {
		
		File volumePageCount_txt = new File( HTRCConfigManager.getProperties("HTRC_VOLUME_CHAR_WORD_COUNT_FILE") );
			
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(volumePageCount_txt)));
			
		String line = null;
			
		while((line = br.readLine()) != null){
			
			String[] ID_CharCount_WordCount = line.split("	");
			
			String volumeID = pt.uncleanId(ID_CharCount_WordCount[0]);
			
			volumeid2charcountmap.put(volumeID, ID_CharCount_WordCount[1]);
			volumeid2wordcountmap.put(volumeID, ID_CharCount_WordCount[2]);
		}
		br.close();
	}


	///////////////////////bib info index
	public HTRCIndexer(String indexingPropsFile, String[] propertyDirs) {
		super(indexingPropsFile, propertyDirs);
		// TODO Auto-generated constructor stub
	}


	public Set<String> getSerialTitle(final Record record, String fieldSpec, String separator){
		 
		Set<String> set = getFieldList(record, fieldSpec);
		String mapName = this.loadTranslationMap("htrc_format_map.properties");
		
		Set<String> format_abbr = getFieldList(record, "970a");
		Set<String> format = Utils.remap(format_abbr, findMap(mapName), true);
		
		for(String s : format){
			if(s.equals("Serial")){
				return set;
			}
		}
		
		return null;
	 }
	 
	 public Set<String> getHTSource(final Record record){
		 
		 Set<String> set = getFieldList(record, "974a");
		 
		 Set<String> retSet = new HashSet<String>();
		 
		 for(String id : set){
			 String prefix = id.split("\\.",2)[0];
			 retSet.add(prefix);
		 }
		 
		 return retSet;
	 }
	 
	 public Set<String> getISBN(Record record, String fieldSpec){
		 
		Set<String> candidates = getFieldList(record, fieldSpec); 
		return Utils.returnValidISBNs(candidates);
	 }
	 
	 public String getPublishDate(final Record record, String fieldSpec){
		 
		 String date = getFirstFieldVal(record, fieldSpec);
		 
		 if(date.contains("u")){
			 date = date.replace("u", "0");
		 }
		 
		 if(date.contains("?")){
			 date = date.replace("?", "0");
		 }
		 
		 if(date.contains(" ")){
			 date = date.replace(" ", "0");
		 }
		 
		 if(date.contains("|")){
			 date = date.replace("|", "0");
		 }
		 
		 if(date.contains("a")){
			 date = date.replace("a", "0");
		 }
		 
		 if(date.contains("i")){
			 date = date.replace("i", "0");
		 }
		 
		 if(date.contains("-")){
			 date = date.replace("-", "0");
		 }
		 
		 return date;
	 }
	 
	 
	 // methods to get HTRC metadata that is not there in MARC records
	 
	 public String getPageCount(final Record record){
		 
		 String volumeID = getFirstFieldVal(record, "974a");
		 
		 return volumeID2PageCountMap.get(volumeID);
		
	 } 
	 
	 public String getWordCount(final Record record){
		 
		 String volumeID = getFirstFieldVal(record, "974a");
		 
		 return volumeID2WordCountMap.get(volumeID);
	 }

	 public String getCharCount(final Record record){
		 
		 String volumeID = getFirstFieldVal(record, "974a");
		 
		 return volumeID2CharCountMap.get(volumeID);
	 }

	 
	 public Set<String> getGenderMale(final Record record){
		 
		 Set<String> resultSet = new HashSet<String>();
		 
		 Set<String> authorSet = getAllSubfields(record, "100[a-d]:700[a-d]", " ");
		// pw.println( "GenderMale: " + authorSet );
		// pw.flush();
		 for(String authorName : authorSet){
			 
			 String gender = authoName2GenderMap.get(authorName);
			 
			 if(gender != null && gender.equals("male")){
				 resultSet.add(authorName);
			 }
		 }
		 return resultSet;
	 }
	 
	 public Set<String> getGenderFemale(final Record record){
		 
		 Set<String> resultSet = new HashSet<String>();
		 
		 Set<String> authorSet = getAllSubfields(record, "100[a-d]:700[a-d]", " ");
		// pw.println( "GenderFeMale: " + authorSet );
		// pw.flush();
		 for(String authorName : authorSet){
			 
			 String gender = authoName2GenderMap.get(authorName);
			 
			 if(gender != null && gender.equals("female")){
				 resultSet.add(authorName);
			 }
		 }
		 return resultSet;
	 }
	 
	 public Set<String> getGenderUnknown(final Record record){
		 
		 Set<String> resultSet = new HashSet<String>();
		 
		 Set<String> authorSet = getAllSubfields(record, "100[a-d]:700[a-d]", " ");
		// pw.println( "GenderFeMale: " + authorSet );
		// pw.flush();
		 for(String authorName : authorSet){
			 
			 String gender = authoName2GenderMap.get(authorName);
			 
			 if(gender == null){
				 resultSet.add(authorName);
			 }
		 }
		 return resultSet;
	 }
	 
	 public Set<String> getGender(final Record record){
		 
		 Set<String> resultSet = new HashSet<String>();
		 
		 Set<String> authorSet = getAllSubfields(record, "100[a-d]:700[a-d]", " ");
		// pw.println("AuthorSet: " + authorSet);
		// pw.flush();
		 
		 for(String authorName : authorSet){
			 
			 String gender = authoName2GenderMap.get(authorName);
			 if(gender == null){
				 continue;
			 }else if(gender.equals("female")){
				 resultSet.add("female");
			 }else if(gender.equals("male")){
				 resultSet.add("male");
			 }
		 }
		 return resultSet;
	 }
	 
	 public String getVolumePageCountBin(final Record record){
		 
		 String volumeID = getFirstFieldVal(record, "974a");
		 
		 int pageNum = Integer.valueOf(volumeID2PageCountMap.get(volumeID));
		 
		 if(pageNum <= pageBinDivider1){
			 return "S";
		 }else if(pageNum <= pageBinDivider2){
			 return "M";
		 }else if(pageNum <= pageBinDivider3){
			 return "L";
		 }else{
			 return "XL";
		 }
	 } 
	 
	 public String getVolumeWordCountBin(final Record record){
		 
		 String volumeID = getFirstFieldVal(record, "974a");
		 
		 int wordCount = Integer.valueOf(volumeID2WordCountMap.get(volumeID));
		 
		 if(wordCount <= wordBinDivider1){
			 return "S";
		 }else if(wordCount <= wordBinDivider2){
			 return "M";
		 }else if(wordCount <= wordBinDivider3){
			 return "L";
		 }else{
			 return "XL";
		 }
	 } 
	 
	 public static class HTRCConfigManager{
		 
		private static Properties properties= new Properties();
		
		static{
			try {
				properties.load(new FileInputStream("../htrc-metadata.config"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public static String getProperties(String key) {
			return properties.getProperty(key);
		}

	 }
}
