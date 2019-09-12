package com.problem.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.problem.dto.Page;
import com.problem.dto.ResultData;
import com.problem.entity.Question;
import com.problem.vo.QuestionVO;
import com.problem.vo.UpdateQuestionVO;
import com.problem.vo.UpdateStatusVO;

public interface QuestionDao {
	//1�������
	void insert(@Param("q")Question q);  //ok
	//2��ѯ���� 3����ѯ��  page ,pagesize =>  limit page*pageSize,pageSize;
	//���أ�{total:20,data:[{},{}]}
	/**
	 * ���ݷ�����ҡ�����ҳ
	 * @param type
	 * @param page
	 * @param pageSize
	 * @return
	 */
	Map<String,Object> getByType(String type,int page,int pageSize);
	
	/**
	 * 
	 * @param type
	 * @param page
	 * @param pageSize
	 * @return
	 */
	Map<String,Object> getByKeyWord(String keyword,int page,int pageSize);
	
	
	
	//3ɾ�����⡣����id��
	void deleteById(int id);
	
	Question findById(int id);
	
	// ids = 1,2,3,4
	void deleteByIdList(String []strs);
	
	
	//4����״̬������id����status��
	void updateStatus(@Param("p")UpdateStatusVO vo);
	/**
	 * ��������id���´𰸡�
	 * @param id
	 * @param status
	 */
	void updateAnswerById(int id,String answer,String animg);
	
	ResultData getAllByPage(Page page);
	
	
	List<Question> getByCondition(QuestionVO qvo);
	
	/**
	 * ���´𰸡�״̬��   ok
	 * @param updatevo
	 */
	void udpateAnswer(@Param("p")UpdateQuestionVO updatevo);
	
}
