package com.zjy.seckill.mapper;

import com.zjy.seckill.dataobject.UserDO;
import org.apache.ibatis.annotations.Param;

public interface UserDOMapper {

    UserDO selectByTelPhone(@Param("telPhone") String telPhone);

    int deleteByPrimaryKey(Integer id);

    int insert(UserDO record);

    int insertSelective(UserDO record);

    UserDO selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(UserDO record);

    int updateByPrimaryKey(UserDO record);
}