package com.example.demo.controllers;

import com.example.demo.models.*;
import com.example.demo.repositories.*;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.EmailAttachment;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import com.example.demo.Utilities;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmailAttachment;
import it.ozimov.springboot.mail.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Controller
public class AdminController
{
	@Autowired
	AuthorityRepo authorityRepo;
	@Autowired
	PersonRepo personRepo;
	@Autowired
	CourseRepo courseRepo;
	@Autowired
	AttendanceRepo attendanceRepo;
	@Autowired
	RegistrationTimestampRepo registrationTimestampRepo;
	@Autowired
	EvaluationRepo evaluationRepo;
	@Autowired
	CourseInfoRequestLogRepo courseInfoRequestLogRepo;


	@GetMapping("/addcourse")
	public String addCourse(Model model)
	{
		model.addAttribute("course", new Course());
		model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
		return "addcourse";
	}


	// fires when adding new course OR updating existing course
	@PostMapping("/addcourse")
	public String submitCourse(@Valid @ModelAttribute("course") Course course, BindingResult result,
							   @RequestParam(value = "selectedTeacher")long teacherId, Model model) {

//		System.out.println("========================= in /addcourse POST, getDiffInDays(startDate, endDate): " + Utilities.getDiffInDays(course.getDateStart(), course.getDateEnd()));

		if(result.hasErrors()) {
			model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
			return "addcourse";
		}

		if(Utilities.getDiffInDays(course.getDateStart(), course.getDateEnd()) < 0) {
			// do not allow a course to be added if start date is after end date
			System.out.println("========================= in /addcourse POST, NEGATIVE NUMBER OF DAYS DETECTED!!!: ");
			model.addAttribute("negativeDayCount", true);
			model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
			return "addcourse";
		}

		// I realized we have to allow admin to edit the course, because that's what the requirements state
//		if(courseRepo.countByCourseRegistrationNumIs(course.getCourseRegistrationNum()) > 0) {
//		    // admin enterd a CRN that already exists, so display an error msg
//            model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
//            model.addAttribute("crnExists", true);
//            return "addcourse";
//        }


		// find out what Person was just selected (by the admin) from the drop down list for this course
		// and set them as the teacher to this course, then save the course

			course.addPerson(personRepo.findOne(teacherId));
			course.setDeleted(false);
			courseRepo.save(course);
			model.addAttribute("teacher", personRepo.findOne(teacherId));

		return "courseconfirm";
	}


	// a course can have the same CRN number, but on different dates
	// this page allows the admin to duplicate a course, it's like adding a new one, but the CRN number stays the same
	@GetMapping("/addduplicatecourse")
	public String addDuplicateCourse(Model model)
	{
		Course cour=new Course();
		// need dummy data for CRN and name or validation is problematic in post route, both these are set again in post route
		// so it doesn't matter what you set them to here
		cour.setCourseRegistrationNum("12345678");
		cour.setName("fakeName");

//		Set<Course> courseSet = courseRepo.findByDeletedIs(0);
//		System.out.println("============================================================ courseSet.size: " + courseSet.size());

		// TODO it shows multiple listings of same course name, not what we want here.. doesn't matter for project specs
		// this is a "nice to do" thing, don't spend time on this until we finish all requirements
		model.addAttribute("courses", courseRepo.findByDeletedIs(false));
//		model.addAttribute("courses", courseRepo.findDistinctFirstByCourseRegistrationNumAndDeletedIs(crns,false));
		model.addAttribute("course",cour);
		model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
		return "addduplicatecourse";
	}

	@PostMapping("/addduplicatecourse")
	public String submitDuplicateCourse(@Valid @ModelAttribute("course")Course course,BindingResult bindingResult,
										@RequestParam(value = "selectCourse")long courseId, @RequestParam(value = "selectedTeacher")long teacherId,
										Model model) {
		if(bindingResult.hasErrors()){
			model.addAttribute("courses", courseRepo.findByDeletedIs(false));
			model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
			return"addduplicatecourse";
		}

		if(Utilities.getDiffInDays(course.getDateStart(), course.getDateEnd()) < 0) {
			// do not allow a course to be added if start date is after end date
			System.out.println("========================= in /addduplicatecourse POST, NEGATIVE NUMBER OF DAYS DETECTED!!!: ");
			model.addAttribute("negativeDayCount", true);
			model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
			return "addduplicatecourse";
		}

		course.setName(courseRepo.findOne(courseId).getName());
		course.setCourseRegistrationNum(courseRepo.findOne(courseId).getCourseRegistrationNum());
		course.addPerson(personRepo.findOne(teacherId));
		course.setDeleted(false);
		model.addAttribute("course",courseRepo.save(course));
		model.addAttribute("teacher", personRepo.findOne(teacherId));

		return "courseconfirm";
	}

	@GetMapping("/editcourse/{courseid}")
	public String editCourse(@PathVariable ("courseid") long id, Model model)
	{
		model.addAttribute("course", courseRepo.findOne(id));
		model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
		return "addcourse";
	}

	@RequestMapping("/deletecourse/{courseid}")
	public String deleteCourse(@PathVariable ("courseid") long id)
	{
		// not tested yet!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		Course course = courseRepo.findOne(id);
		course.setDeleted(true);
		courseRepo.save(course);

		return "redirect:/allcourses";
	}

	// shows a list of all courses, admin can do various things from the list
	// such as edit, delete, view details, and log info requests
	@RequestMapping("/allcourses")
	public String allCourses(Model model)
	{
		model.addAttribute("allcourses", courseRepo.findByDeletedIs(false));
		return "allcourses";
	}

	@RequestMapping("/viewdeletedcourses")
	public String deletedCourses(Model model)
	{
		model.addAttribute("allcourses", courseRepo.findByDeletedIs(true));
		return "viewdeletedcourses";
	}


	// view all the students, admin can click on one to see the courses that particular student is registered in
	// this route shows the list of students only
	@GetMapping("/allstudents")
	public String allStudents(Model model) {
		// TODO implement this... HIWOT CAN YOU TAKE CARE OF THIS?
		// TODO need to make another route that will show the courses that the student is registered in, ie what student was clicked from this routes table of students
		model.addAttribute("students", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("STUDENT")));
		return "allstudents";
	}
	//List of courses for a particular Student
	@RequestMapping("/viewcoursetakenbystudent/{id}")
	public String listStudentCourses(@PathVariable("id") long studentId,Model model) {
		Person student = personRepo.findOne(studentId);
		model.addAttribute("studentcourse", student);
		model.addAttribute("courselist", courseRepo.findByPersonsIsAndDeletedIs(student, false));
		return "viewcoursetakenbystudent";
	}


	// view all the teachers, admin can click on one to see that teachers evaluations
	@GetMapping("/allteachers")
	public String allTeachers(Model model) {

		model.addAttribute("teachers", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("TEACHER")));
		return "allteachers";
	}


	// view all the evaluations for a single course
	@GetMapping("/viewcourseevaluations/{id}")
	public String viewEvalsForOneCourse(@PathVariable("id") long courseId, Model model) {

		// dummy testing data is commented out, but you can use it if you want, just make sure you comment
		// out the line - model.addAttribute("evaluations", evaluationRepo.findByCourseIs(course));

//		// create some dummy data for testing ================================================================
//		Set<Evaluation> testSet = new HashSet<>();
//
//		for(int i = 0; i < 30; i++) {
//			Evaluation eval = new Evaluation();
//			Course course = new Course();
//			course.setName("Astro Physics 200");
//			course.setDeleted(false);
//			course.setCourseRegistrationNum(987934534);
//			course.setDateStart(new Date());
//			course.setDateEnd(new Date());
//
//			eval.setCourse(course);
//			eval.setCourseContentRating("Above Average");
//			eval.setInstructionQualityRating("Excellent");
//			eval.setTrainingExperienceRating("Average");
//			eval.setTextBookRating("Fair");
//			eval.setClassroomEnvironment("Poor");
//			eval.setEquipmentRating("Poor");
//			eval.setWhatDidYouLike("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatDidntYouLike("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatImprovements("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatOtherClasses("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatOtherClasses("Internet/Website");
//			testSet.add(eval);
//		}
//		model.addAttribute("evaluations", testSet);
//		// end dummy data for testing ================================================================



		Course course = courseRepo.findOne(courseId);
		model.addAttribute("courseName", course.getName());

		// get all the evaluations for a single course
		model.addAttribute("evaluations", evaluationRepo.findByCourseIs(course));
		return "viewcourseevaluations";
	}


	// view all the evaluations for a single teacher in one gigantic table
	@GetMapping("/viewteacherevaluations/{id}")
	public String viewEvalsForOneTeacher(@PathVariable("id") long teacherId, Model model) {

		// dummy testing data is commented out, but you can use it if you want, just make sure you comment
		// out the line - model.addAttribute("evaluations", evaluationRepo.findByCourseIs(course));

//		// create some dummy data for testing ================================================================
//		Set<Evaluation> testSet = new HashSet<>();
//
//		for(int i = 0; i < 30; i++) {
//			Evaluation eval = new Evaluation();
//			Course course = new Course();
//			course.setName("Astro Physics 200");
//			course.setDeleted(false);
//			course.setCourseRegistrationNum(987934534);
//			course.setDateStart(new Date());
//			course.setDateEnd(new Date());
//
//			eval.setCourse(course);
//			eval.setCourseContentRating("Above Average");
//			eval.setInstructionQualityRating("Excellent");
//			eval.setTrainingExperienceRating("Average");
//			eval.setTextBookRating("Fair");
//			eval.setClassroomEnvironment("Poor");
//			eval.setEquipmentRating("Poor");
//			eval.setWhatDidYouLike("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatDidntYouLike("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatImprovements("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatOtherClasses("it was a lot of fun argle bargle lorem ipsum blarg blarck");
//			eval.setWhatOtherClasses("Internet/Website");
//			testSet.add(eval);
//		}
//		model.addAttribute("evaluations", testSet);
//		// end dummy data for testing ================================================================




//		model.addAttribute("evaluations", evaluationRepo.findByPersonIsOrderByCourseAsc(personRepo.findOne(teacherId)));


		Person teacher = personRepo.findOne(teacherId);
		model.addAttribute("teacherName", teacher.getFullName());
		model.addAttribute("evaluations", evaluationRepo.findByPersonIs(teacher));

		return "viewteacherevaluations";
	}


//	// this is where the admin can log information requests about a course
	@GetMapping("/loginforequest/{id}")
	public String logInfoRequestGet(@PathVariable("id") long courseId, Model model) {

		// note that CourseInfoRequestLog has info about person and course

//		model.addAttribute("course", courseRepo.findOne(courseId));
		CourseInfoRequestLog logObject = new CourseInfoRequestLog();
		logObject.setCourse(courseRepo.findOne(courseId));
		model.addAttribute("courseInfoLog", logObject);
		return "loginforequestform";
	}


	// note: the course id is preserved through the form, so does not need to be set again here
	@PostMapping("/loginforequest")
	public String logInfoRequestPost(@RequestParam("mnum") String enteredMnum,
									 @Valid @ModelAttribute("courseInfoLog") CourseInfoRequestLog log,
									 BindingResult bindingResult, Model model) {

		// validates email field (if anything entered), validates description for not empty
		if(bindingResult.hasErrors()) {

			return "loginforequestform";
		}

		// manually check if BOTH email and contact num were empty
		if(log.getContactNum().isEmpty() && log.getEmail().isEmpty()) {
			model.addAttribute("noEmailAndNoContactNum", true);
			return "loginforequestform";
		}

		// if admin entered an mnum, check to make sure it's valid
		if(!enteredMnum.isEmpty()) {
			if(personRepo.countByMNumberIs(enteredMnum) == 0) {
				// there was no student with that mnumber, display error msg
				model.addAttribute("badMnum", true);
				return "loginforequestform";
			}
			else {
				// found a match for that mnum, so save to db
				log.setPerson(personRepo.findByMNumberIs(enteredMnum));
				courseInfoRequestLogRepo.save(log);
				model.addAttribute("message", "New course info request log saved");
				model.addAttribute("extraMessage", String.format("Course: %s - Existing student: %s",
						courseRepo.findOne(log.getCourse().getId()).getName(),
						personRepo.findByMNumberIs(enteredMnum).getFullName(),
						personRepo.findByMNumberIs(enteredMnum).getEmail(),
				personRepo.findByMNumberIs(enteredMnum).getContactNum()));
				return "loginforequestconfirmation";
			}
		}

		// at this point, we are saving a new request log, but not for an existing student
		courseInfoRequestLogRepo.save(log);
		model.addAttribute("message", "New course info request log saved");
		model.addAttribute("extraMessage", String.format("Course: %s",
				courseRepo.findOne(log.getCourse().getId()).getName()));

		return "loginforequestconfirmation";
	}


	//List of log infor request for a particular course
	@RequestMapping("/loginforequestdetail/{id}")
	public String listCourseInfoReq(@PathVariable("id") long courseId,Model model) {
		Course course = courseRepo.findOne(courseId);
		model.addAttribute("courseinforeq", course);
		model.addAttribute("inforeqlist", courseInfoRequestLogRepo.findByCourseIs(course));
		return "loginforequestdetail";
	}

	//edit log info request
	@RequestMapping("/editloginforequest/{inforeqid}")
	public String editCourseInfoReq(@PathVariable ("inforeqid") long id, Model model)
	{
		model.addAttribute("courseInfoLog", courseInfoRequestLogRepo.findOne(id));
		return "loginforequestform";
	}

	//delete log info request
	@RequestMapping("/deleteloginforequest/{id}")
	public String delCourseInfoReq(@PathVariable("id") long id)
	{
		courseInfoRequestLogRepo.delete(id);
		return "redirect:/allcourses";
	}



}
