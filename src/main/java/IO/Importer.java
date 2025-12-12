package IO;

import Core.ClassRoom;
import Core.Course;
import Core.Student;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Importer {

	
	public static List<Student> importStudents(Path filePath) {
		List<Student> students = new ArrayList<>();
		if (!Validator.validateFile(filePath)) return students;
		// read the file line-by-line and construct Student objects
		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine == null) continue;
				currentLine = currentLine.trim();
				if (currentLine.isEmpty()) continue;
				if (currentLine.startsWith("Std_ID")) {
					String extractedId = extractTrailingDigitsString(currentLine);
					if (extractedId != null) {
						try {
							// the Student class stores an integer ID; parse safely
							int idInt = Integer.parseInt(extractedId);
							students.add(new Student(idInt, ""));
						} catch (NumberFormatException nfe) {
							// skip invalid numeric id (malformed token)
						}
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Parsing is wrong at students file: " + filePath + " - " + e.getMessage());
		}
		return students;
	}

	
	public static List<Course> importCourses(Path filePath) {
		List<Course> courses = new ArrayList<>();
		if (!Validator.validateFile(filePath)) return courses;
		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine == null) continue;
				currentLine = currentLine.trim();
				if (currentLine.isEmpty()) continue;
				if (currentLine.startsWith("CourseCode")) {
					String extractedId = extractTrailingDigitsString(currentLine);
					if (extractedId != null) courses.add(new Course(extractedId));
				}
			}
		} catch (IOException e) {
			System.out.println("Parsing is wrong at courses file: " + filePath + " - " + e.getMessage());
		}
		return courses;
	}

	
	public static List<ClassRoom> importClassrooms(Path filePath) {
		List<ClassRoom> classrooms = new ArrayList<>();
		if (!Validator.validateFile(filePath)) return classrooms;
		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine == null) continue;
				currentLine = currentLine.trim();
				if (currentLine.isEmpty()) continue;
				// expect lines like: Classroom_01;40 (name;capacity)
				if (!currentLine.contains(";")) continue;
				String[] parts = currentLine.split(";", 2);
				String classroomName = parts[0].trim();
				String capacityStr = parts.length > 1 ? parts[1].trim() : "";
				try {
					int capacity = Integer.parseInt(capacityStr);
					classrooms.add(new ClassRoom(classroomName, capacity));
				} catch (NumberFormatException nfe) {
					// skip malformed capacity lines (keep parsing remaining lines)
				}
			}
		} catch (IOException e) {
			System.out.println("Parsing is wrong at classrooms file: " + filePath + " - " + e.getMessage());
		}
		return classrooms;
	}

	public static List<Course> importAttendanceLists(Path filePath, List<Course> existingCourses) {
		return importAttendanceLists(filePath, existingCourses, null);
	}

	
	public static List<Course> importAttendanceLists(Path filePath, List<Course> existingCourses, List<Student> knownStudents) {
		Map<String, Course> courseById = new HashMap<>();
		if (existingCourses != null) {
			for (Course c : existingCourses) courseById.put(c.getID(), c);
		}
		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String currentLine;
			String currentCourseId = null;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine == null) continue;
				currentLine = currentLine.trim();
				if (currentLine.isEmpty()) continue;
				if (currentLine.startsWith("CourseCode")) {
					String courseId = extractTrailingDigitsString(currentLine);
					if (courseId != null) {
						// If caller provided an explicit course list, only accept those
						if (existingCourses != null && !courseById.containsKey(courseId)) {
							System.out.println("Unknown course in attendance file: " + courseId + " (" + filePath + ")");
							currentCourseId = null; // skip the following student list
						} else {
							currentCourseId = courseId;
							courseById.putIfAbsent(currentCourseId, new Course(currentCourseId));
						}
					} else {
						currentCourseId = null;
					}
					continue;
				}
				if (currentCourseId != null) {
					// parse the Python-style student list and add each student ID
					List<String> enrolledStudentIds = parseStrings(currentLine);
					Course course = courseById.get(currentCourseId);
					if (course != null) {
						for (String sid : enrolledStudentIds) {
							boolean skipStudent = false;
							if (knownStudents != null) {
								try {
									int sidInt = Integer.parseInt(sid);
									boolean exists = knownStudents.stream().anyMatch(s -> s.getID() == sidInt);
									if (!exists) {
										System.out.println("Unknown student referenced in attendance file: " + sid + " for course " + currentCourseId + " (" + filePath + ")");
										skipStudent = true;
									}
								} catch (NumberFormatException nfe) {
									System.out.println("Malformed student id in attendance file: " + sid + " for course " + currentCourseId + " (" + filePath + ")");
									skipStudent = true;
								}
							}
							if (!skipStudent) course.addEnrolledStudentID(sid);
						}
					}
					currentCourseId = null;
				}
			}
		} catch (IOException e) {
			System.out.println("Parsing is wrong at attendance file: " + filePath + " - " + e.getMessage());
		}
		return new ArrayList<>(courseById.values());
	}

	// Parses a Python-style list string like: ['Std_ID_170', 'Std_ID_077', ...]
	private static List<String> parseStrings(String input) {
		List<String> result = new ArrayList<>();
		if (input == null) return result;
		String trimmed = input.trim();
		if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
		if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
		if (trimmed.trim().isEmpty()) return result;
		String[] elements = trimmed.split(",");
		for (String token : elements) {
			String tokenValue = token.trim();
			if (tokenValue.startsWith("'") && tokenValue.endsWith("'") && tokenValue.length() >= 2) {
				tokenValue = tokenValue.substring(1, tokenValue.length() - 1);
			} else if (tokenValue.startsWith("\"") && tokenValue.endsWith("\"") && tokenValue.length() >= 2) {
				tokenValue = tokenValue.substring(1, tokenValue.length() - 1);
			}
			// extract trailing digits but preserve leading zeros, return whole digits string
			String extractedId = extractTrailingDigitsString(tokenValue);
			if (extractedId != null) result.add(extractedId);
		}
		return result;
	}

	// Extracts trailing digit sequence from a token like "Std_ID_123" or "CourseCode_04" and preserves leading zeros (returns string)
	private static String extractTrailingDigitsString(String value) {
		if (value == null) return null;
		// find last sequence of digits in the token
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(value);
		String lastDigits = null;
		while (matcher.find()) lastDigits = matcher.group(1);
		return lastDigits;
	}

}