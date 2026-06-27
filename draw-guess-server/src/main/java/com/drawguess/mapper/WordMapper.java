package com.drawguess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drawguess.model.entity.Word;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordMapper extends BaseMapper<Word> {
}
