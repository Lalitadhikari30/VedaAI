import teacherVideo from '../../assets/hd_mei_do_yrr.mp4';

export default function TeacherIllustration() {
  return (
    <div className="relative w-52 h-52 md:w-60 md:h-60 mx-auto my-2 md:my-3 flex items-center justify-center select-none">
      {/* Outer soft peach circular gradient glow */}
      <div
        className="absolute inset-0 rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(251, 228, 216, 0.95) 0%, rgba(251, 228, 216, 0.45) 50%, rgba(240, 240, 238, 0) 72%)',
        }}
      />

      {/* Delicate outer ring border */}
      <div
        className="absolute w-44 h-44 md:w-50 md:h-50 rounded-full border border-[#F5C7A9]/60"
      />

      {/* Main circular avatar video container */}
      <div className="relative w-38 h-38 md:w-44 md:h-44 rounded-full bg-gradient-to-b from-[#FFF5F0] via-[#FDECE2] to-[#FCDCC9] border-[3.5px] border-white shadow-lg flex items-center justify-center overflow-hidden">
        {/* Seamless, muted autoplay looping video */}
        <video
          src={teacherVideo}
          autoPlay
          loop
          muted
          playsInline
          className="w-full h-full object-cover"
        />
      </div>
    </div>
  );
}
