import { Clock, Image as ImageIcon, CloudUpload, Settings } from 'lucide-react';

export default function TeacherIllustration() {
  return (
    <div className="relative w-56 h-56 md:w-64 md:h-64 mx-auto my-2 md:my-4 flex items-center justify-center select-none">
      {/* Outer soft peach circular gradient glow matching reference */}
      <div
        className="absolute inset-0 rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(251, 228, 216, 0.95) 0%, rgba(251, 228, 216, 0.5) 50%, rgba(240, 240, 238, 0) 72%)',
        }}
      />
      {/* Delicate outer ring border */}
      <div
        className="absolute w-44 h-44 md:w-50 md:h-50 rounded-full border border-[#F5C7A9]/60"
      />

      {/* Main circular avatar container */}
      <div className="relative w-34 h-34 md:w-40 md:h-40 rounded-full bg-gradient-to-b from-[#FFF5F0] via-[#FDECE2] to-[#FCDCC9] border-[2.5px] border-white shadow-md flex items-center justify-center overflow-hidden">
        {/* Professional 3D-styled Female Teacher Illustration */}
        <svg viewBox="0 0 160 160" className="w-full h-full transform translate-y-1.5 scale-105">
          <defs>
            <linearGradient id="hairDark" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#352018" />
              <stop offset="60%" stopColor="#1E100B" />
              <stop offset="100%" stopColor="#0F0705" />
            </linearGradient>
            <linearGradient id="hairHighlight" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#5A3A2C" />
              <stop offset="50%" stopColor="#734B39" />
              <stop offset="100%" stopColor="#5A3A2C" />
            </linearGradient>
            <linearGradient id="skinTone" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#FFE0C8" />
              <stop offset="50%" stopColor="#F5C6A5" />
              <stop offset="100%" stopColor="#E2A680" />
            </linearGradient>
            <linearGradient id="blazerNavy" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#2D3139" />
              <stop offset="100%" stopColor="#15171C" />
            </linearGradient>
            <linearGradient id="innerShirt" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#FFFFFF" />
              <stop offset="100%" stopColor="#E2E8F0" />
            </linearGradient>
            <linearGradient id="binderWhite" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FFFFFF" />
              <stop offset="100%" stopColor="#F1F5F9" />
            </linearGradient>
            <filter id="shadowFilter" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="2" stdDeviation="2.5" floodColor="#000000" floodOpacity="0.18" />
            </filter>
          </defs>

          {/* Back Hair Volume with highlights */}
          <path d="M48 48 Q80 16 112 48 Q124 75 118 104 Q106 116 100 118 Q95 86 92 74 Q80 77 68 74 Q65 86 60 118 Q54 116 42 104 Q36 75 48 48 Z" fill="url(#hairDark)" />

          {/* Neck */}
          <rect x="71" y="78" width="18" height="24" rx="4" fill="url(#skinTone)" />
          {/* Neck Shadow */}
          <path d="M71 78 Q80 88 89 78 L89 84 Q80 93 71 84 Z" fill="#D4946F" opacity="0.65" />

          {/* Head Shape */}
          <ellipse cx="80" cy="56" rx="24" ry="27" fill="url(#skinTone)" filter="url(#shadowFilter)" />

          {/* Styled Front Hair / Bangs */}
          <path d="M54 44 Q80 28 106 44 Q110 56 108 72 Q98 50 80 50 Q62 50 52 72 Q50 56 54 44 Z" fill="url(#hairDark)" />
          <path d="M60 40 Q80 30 100 40 Q94 36 80 36 Q66 36 60 40 Z" fill="url(#hairHighlight)" opacity="0.6" />

          {/* Eyebrows */}
          <path d="M62 48 Q69 45 76 49" fill="none" stroke="#25140E" strokeWidth="2" strokeLinecap="round" />
          <path d="M84 49 Q91 45 98 48" fill="none" stroke="#25140E" strokeWidth="2" strokeLinecap="round" />

          {/* Expressive Eyes */}
          <ellipse cx="69" cy="55" rx="3.5" ry="3.2" fill="#1C1A1A" />
          <circle cx="70.5" cy="54" r="1.2" fill="#FFFFFF" />
          <ellipse cx="91" cy="55" rx="3.5" ry="3.2" fill="#1C1A1A" />
          <circle cx="92.5" cy="54" r="1.2" fill="#FFFFFF" />

          {/* Modern Black Glasses Frame */}
          <rect x="60" y="48" width="18" height="14" rx="3.5" fill="none" stroke="#1A1A1A" strokeWidth="2.2" />
          <rect x="82" y="48" width="18" height="14" rx="3.5" fill="none" stroke="#1A1A1A" strokeWidth="2.2" />
          <line x1="78" y1="54" x2="82" y2="54" stroke="#1A1A1A" strokeWidth="2.2" />
          {/* Glass Glare Reflections */}
          <line x1="62" y1="51" x2="67" y2="60" stroke="#FFFFFF" strokeWidth="1.2" strokeLinecap="round" opacity="0.7" />
          <line x1="84" y1="51" x2="89" y2="60" stroke="#FFFFFF" strokeWidth="1.2" strokeLinecap="round" opacity="0.7" />

          {/* Soft Nose */}
          <path d="M80 56 L78 64 L82 64" fill="none" stroke="#CE8861" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />

          {/* Friendly Warm Smile */}
          <path d="M73 70 Q80 77 87 70" fill="none" stroke="#B84A40" strokeWidth="2.4" strokeLinecap="round" />

          {/* Inner Shirt V-Neck */}
          <path d="M66 92 L80 114 L94 92 L88 86 L72 86 Z" fill="url(#innerShirt)" />

          {/* Dark Business Blazer */}
          <path d="M42 102 Q60 88 80 88 Q100 88 118 102 L130 160 L30 160 Z" fill="url(#blazerNavy)" />
          {/* Blazer Lapels */}
          <path d="M56 94 L74 124 L66 160 L44 160 Z" fill="#1D2026" />
          <path d="M104 94 L86 124 L94 160 L116 160 Z" fill="#1D2026" />

          {/* White Document / Notebook in hands */}
          <rect x="58" y="116" width="44" height="36" rx="4" fill="url(#binderWhite)" stroke="#CBD5E1" strokeWidth="1.6" filter="url(#shadowFilter)" />
          {/* Notebook Orange Accent Line & text lines */}
          <rect x="63" y="122" width="24" height="2.2" rx="1" fill="#E8623C" />
          <line x1="63" y1="129" x2="97" y2="129" stroke="#94A3B8" strokeWidth="1.6" strokeLinecap="round" />
          <line x1="63" y1="135" x2="91" y2="135" stroke="#94A3B8" strokeWidth="1.6" strokeLinecap="round" />
          <line x1="63" y1="141" x2="82" y2="141" stroke="#94A3B8" strokeWidth="1.6" strokeLinecap="round" />

          {/* Thumbs holding book */}
          <ellipse cx="56" cy="130" rx="4" ry="7.5" fill="url(#skinTone)" />
          <ellipse cx="104" cy="130" rx="4" ry="7.5" fill="url(#skinTone)" />
        </svg>
      </div>

      {/* Decorative 4 Badge Icons positioned on the ring border */}
      {/* 1. Top-Right: Clock icon */}
      <div
        className="absolute top-3 right-5 md:top-4 md:right-7 w-7 h-7 md:w-8 md:h-8 rounded-full bg-[#E8623C] text-white flex items-center justify-center shadow-md ring-2 ring-white"
        title="Clock"
      >
        <Clock size={14} strokeWidth={2.5} />
      </div>

      {/* 2. Left: Image / Photo icon */}
      <div
        className="absolute top-1/2 -translate-y-1/2 left-2 md:left-4 w-7 h-7 md:w-8 md:h-8 rounded-full bg-[#E8623C] text-white flex items-center justify-center shadow-md ring-2 ring-white"
        title="Document Image"
      >
        <ImageIcon size={14} strokeWidth={2.5} />
      </div>

      {/* 3. Bottom: Cloud-Upload icon */}
      <div
        className="absolute bottom-2 left-1/2 -translate-x-1/2 md:bottom-3 w-7 h-7 md:w-8 md:h-8 rounded-full bg-[#E8623C] text-white flex items-center justify-center shadow-md ring-2 ring-white"
        title="Upload"
      >
        <CloudUpload size={14} strokeWidth={2.5} />
      </div>

      {/* 4. Right: Gear / Settings icon */}
      <div
        className="absolute top-1/2 -translate-y-1/2 right-2 md:right-4 w-7 h-7 md:w-8 md:h-8 rounded-full bg-[#E8623C] text-white flex items-center justify-center shadow-md ring-2 ring-white"
        title="Settings"
      >
        <Settings size={14} strokeWidth={2.5} />
      </div>
    </div>
  );
}
