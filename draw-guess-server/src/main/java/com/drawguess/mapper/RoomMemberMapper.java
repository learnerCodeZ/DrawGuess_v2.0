package com.drawguess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drawguess.model.entity.RoomMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomMemberMapper extends BaseMapper<RoomMember> {
}
