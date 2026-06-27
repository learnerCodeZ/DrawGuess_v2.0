package com.drawguess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drawguess.model.entity.GameRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GameRecordMapper extends BaseMapper<GameRecord> {

    @Select("SELECT DISTINCT gr.* FROM game_record gr " +
            "INNER JOIN room_member rm ON gr.room_id = rm.room_id " +
            "WHERE rm.user_id = #{userId} " +
            "ORDER BY gr.played_at DESC")
    List<GameRecord> selectByUserId(@Param("userId") Long userId);
}
